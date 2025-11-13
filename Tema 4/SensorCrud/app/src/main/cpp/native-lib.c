#include <jni.h>
#include <string.h>
#include <pthread.h>
#include <android/log.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>

#define LOG_TAG "NativeServer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define PORT 5555

static int server_started = 0;
static pthread_t server_thread;

void *server_loop(void *arg) {
    int server_fd, client_fd;
    struct sockaddr_in addr;
    socklen_t addrlen = sizeof(addr);
    char buffer[1024];

    // Creăm socket-ul
    server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd < 0) {
        LOGE("socket() failed");
        return NULL;
    }

    // Reuse address
    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    // ascultăm doar pe localhost
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    addr.sin_port = htons(PORT);

    if (bind(server_fd, (struct sockaddr*) &addr, sizeof(addr)) < 0) {
        LOGE("bind() failed");
        close(server_fd);
        return NULL;
    }

    if (listen(server_fd, 5) < 0) {
        LOGE("listen() failed");
        close(server_fd);
        return NULL;
    }

    LOGI("C TCP server started on 127.0.0.1:%d", PORT);

    while (1) {
        client_fd = accept(server_fd, (struct sockaddr*) &addr, &addrlen);
        if (client_fd < 0) {
            LOGE("accept() failed");
            continue;
        }

        int n = read(client_fd, buffer, sizeof(buffer) - 1);
        if (n <= 0) {
            close(client_fd);
            continue;
        }
        buffer[n] = '\0';

        // scoatem \r\n dacă există
        char *newline = strchr(buffer, '\n');
        if (newline) *newline = '\0';
        newline = strchr(buffer, '\r');
        if (newline) *newline = '\0';

        LOGI("Received: %s", buffer);

        // protocol text: CREATE|..., DELETE|..., MARK_READ|..., SHAKE|...
        if (strncmp(buffer, "CREATE|", 7) == 0) {
            LOGI("Handled CREATE");
            const char *resp = "OK|CREATE\n";
            write(client_fd, resp, strlen(resp));
        } else if (strncmp(buffer, "DELETE|", 7) == 0) {
            LOGI("Handled DELETE");
            const char *resp = "OK|DELETE\n";
            write(client_fd, resp, strlen(resp));
        } else if (strncmp(buffer, "MARK_READ|", 10) == 0) {
            LOGI("Handled MARK_READ");
            const char *resp = "OK|MARK_READ\n";
            write(client_fd, resp, strlen(resp));
        } else if (strncmp(buffer, "SHAKE", 5) == 0) {
            LOGI("Handled SHAKE: marking all as read (on server side)");
            const char *resp = "OK|SHAKE\n";
            write(client_fd, resp, strlen(resp));
        } else {
            LOGI("Unknown command");
            const char *resp = "ERROR|UNKNOWN\n";
            write(client_fd, resp, strlen(resp));
        }

        close(client_fd);
    }

    close(server_fd);
    return NULL;
}

JNIEXPORT void JNICALL
Java_com_example_sensorcrud_NativeServer_startServer(JNIEnv *env, jclass clazz) {
if (server_started) {
LOGI("Server already started");
return;
}
server_started = 1;

int rc = pthread_create(&server_thread, NULL, server_loop, NULL);
if (rc != 0) {
LOGE("Failed to create server thread");
server_started = 0;
} else {
LOGI("Server thread created");
}
}
