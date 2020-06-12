#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <stdatomic.h>
#include <string.h>
#include <signal.h>
#include <time.h>

#include <wiringPi.h>
#include <mosquitto.h>

// the values received from mosquitto
atomic_int light_level = 0;

// while running
atomic_bool will_exit = false;

void quit() {
    atomic_store(&will_exit, true);
}

bool running() {
    return atomic_load(&will_exit) == false;
}

void handle_signal(int signo) {
    // unused args
    (void)signo;
    quit();
}

void gen_client_id(char *buf, size_t len) {
    srand(time(NULL));
    snprintf(buf, len, "light_%d", rand());
}

void smooth_led_set(int x, int y) {
    if (x == y) {
        return;
    }
    // scale to 1024
    x = x == 0 ? 0 : x * 64 - 1;
    y = y == 0 ? 0 : y * 64 - 1;
    if (x < y) {
        while (x < y) {
            pwmWrite(1, x);
            delay(1);
            x++;
        }
    } else {
        while (x > y) {
            pwmWrite(1, x);
            delay(1);
            x--;
        }
    }
}

void message_callback(struct mosquitto *mosq, void *uo, const struct mosquitto_message *m) {
    // unused args
    (void)mosq;
    (void)uo;

    int light;
    char payload[m->payloadlen+1];

    // this is to ensure we have a zero byte,
    // so scanf doesn't go off into oblivion,
    // reading arbitrary data from memory
    memcpy(payload, m->payload, m->payloadlen);
    payload[m->payloadlen] = '\0';

    sscanf(payload, "{\"light\":%d}", &light);
    atomic_store(&light_level, light);
}

int main() {
    int rc = 0, err;
    struct mosquitto *mosq;

    if (wiringPiSetup() == -1) {
        perror("wiringPiSetup");
        rc = 1;
        goto err_1;
    }

    // init mosquitto
    mosquitto_lib_init();

    // the mosquitto client
    char clientid[64];
    gen_client_id(clientid, sizeof(clientid));

    mosq = mosquitto_new(clientid, true, 0);
    if (!mosq) {
        perror("mosquitto_new");
        rc = 2;
        goto err_2;
    }

    // handler for new light level messages
    mosquitto_message_callback_set(mosq, message_callback);

    // start mosquitto network thread
    err = mosquitto_connect_async(mosq, "broker.hivemq.com", 1883, 60);
    if (err != MOSQ_ERR_SUCCESS) {
        perror("mosquitto_connect_async");
        rc = 3;
        goto err_3;
    }

    // subscribe to the defined topic
    err = mosquitto_subscribe(mosq, NULL, "minecraft/fcup/light-level", 0);
    if (err != MOSQ_ERR_SUCCESS) {
        perror("mosquitto_subscribe");
        rc = 4;
        goto err_4;
    }

    // subscribe to the defined topic
    err = mosquitto_loop_start(mosq);
    if (err != MOSQ_ERR_SUCCESS) {
        perror("mosquitto_loop_start");
        rc = 5;
        goto err_4;
    }

    // signal handlers
    signal(SIGINT, handle_signal);
    signal(SIGTERM, handle_signal);
    signal(SIGQUIT, handle_signal);
    signal(SIGHUP, handle_signal);

    // turn on PWM output mode in physical pin 12,
    // where we will connect our jumper cable;
    //
    // wiringPi has their own pin mapping scheme which
    // translates, in this case, 1 to 12
    pinMode(1, PWM_OUTPUT);

    int old_light = 0;

    while (running()) {
        int new_light = atomic_load(&light_level);
        smooth_led_set(old_light, new_light);
        old_light = new_light;
        delay(1);
    }

    // cleanup mosquitto
err_4:
    mosquitto_disconnect(mosq);
    mosquitto_loop_stop(mosq, false);
err_3:
    mosquitto_destroy(mosq);
err_2:
    mosquitto_lib_cleanup();

    // cleanup wiringPi
    pwmWrite(1, 0);
    pinMode(1, INPUT);

err_1:
    return rc;
}
