pipeline {
    // Runs on the Jenkins host/container itself (built-in node or any available agent)
    agent any

    parameters {
        choice(name: 'OS',   choices: ['linux', 'darwin', 'windows'], description: 'Target operating system')
        choice(name: 'ARCH', choices: ['amd64', 'arm64'],             description: 'Target architecture')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip running tests')
        booleanParam(name: 'SKIP_LINT',  defaultValue: false, description: 'Skip running linter')
        booleanParam(name: 'PUSH_IMAGE', defaultValue: false, description: 'Build and push Docker image to ghcr.io (needs Docker on the agent and the "ghcr-token" credential)')
    }

    environment {
        REPO       = 'https://github.com/chumachenko-vy/kbot'
        BRANCH     = 'main'
        REGISTRY   = 'ghcr.io'
        GO_VERSION = '1.26.1'
        // Go toolchain and caches live in the workspace, so nothing has to be preinstalled on the agent
        GOROOT     = "${WORKSPACE}/.go"
        GOPATH     = "${WORKSPACE}/.gopath"
        GOCACHE    = "${WORKSPACE}/.gocache"
        PATH       = "${WORKSPACE}/.go/bin:${env.PATH}"
    }

    options {
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Clone') {
            steps {
                echo 'CLONE REPOSITORY'
                git branch: "${BRANCH}", url: "${REPO}"
            }
        }

        stage('Setup Go') {
            steps {
                echo "SETUP GO ${GO_VERSION}"
                sh '''
                    if [ ! -x "$GOROOT/bin/go" ]; then
                        case "$(uname -m)" in
                            x86_64)        HOST_ARCH=amd64 ;;
                            aarch64|arm64) HOST_ARCH=arm64 ;;
                            *) echo "Unsupported host arch: $(uname -m)"; exit 1 ;;
                        esac
                        mkdir -p "$GOROOT"
                        curl -fsSL "https://go.dev/dl/go${GO_VERSION}.linux-${HOST_ARCH}.tar.gz" \
                            | tar -xz -C "$GOROOT" --strip-components=1
                    fi
                    go version
                '''
            }
        }

        stage('Lint') {
            when { expression { !params.SKIP_LINT } }
            steps {
                echo 'LINT'
                sh 'go vet ./...'
            }
        }

        stage('Test') {
            when { expression { !params.SKIP_TESTS } }
            steps {
                echo 'TEST'
                sh 'go test -v ./...'
            }
        }

        stage('Build') {
            steps {
                echo "BUILD FOR ${params.OS}/${params.ARCH}"
                sh '''
                    VERSION=$(git describe --tags --abbrev=0 2>/dev/null || echo v1.0.0)-$(git rev-parse --short HEAD)
                    EXT=""
                    [ "$OS" = "windows" ] && EXT=".exe"
                    mkdir -p dist
                    CGO_ENABLED=0 GOOS="$OS" GOARCH="$ARCH" go build -v \
                        -o "dist/kbot-${VERSION}-${OS}-${ARCH}${EXT}" \
                        -ldflags "-X=github.com/chumachenko-vy/kbot/cmd.appVersion=${VERSION}"
                    ls -l dist
                '''
                archiveArtifacts artifacts: 'dist/*', fingerprint: true
            }
        }

        stage('Image') {
            when { expression { params.PUSH_IMAGE } }
            steps {
                echo "BUILD IMAGE ${params.OS}/${params.ARCH}"
                sh 'make image TARGETOS=$OS TARGETARCH=$ARCH'
            }
        }

        stage('Push') {
            when { expression { params.PUSH_IMAGE } }
            steps {
                echo 'PUSH IMAGE'
                withCredentials([usernamePassword(credentialsId: 'ghcr-token',
                                                  usernameVariable: 'GHCR_USER',
                                                  passwordVariable: 'GHCR_TOKEN')]) {
                    sh '''
                        echo "$GHCR_TOKEN" | docker login "$REGISTRY" -u "$GHCR_USER" --password-stdin
                        make push TARGETOS=$OS TARGETARCH=$ARCH
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'if [ "$PUSH_IMAGE" = "true" ]; then make clean TARGETOS=$OS TARGETARCH=$ARCH || true; fi'
        }
    }
}
