package com.example.sensorcrud

object NativeServer {

    init {
        System.loadLibrary("native-lib")
    }

    external fun startServer()
}
