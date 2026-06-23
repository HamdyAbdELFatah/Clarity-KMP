package com.hamdy.clarity.consumer

import android.content.Context
import com.hamdy.clarity.ClarityClient
import com.hamdy.clarity.ClarityConfig
import com.hamdy.clarity.createClarityClient

fun androidConsumerSmoke(context: Context): ClarityClient =
    createClarityClient(context, ClarityConfig("consumer-test"))
