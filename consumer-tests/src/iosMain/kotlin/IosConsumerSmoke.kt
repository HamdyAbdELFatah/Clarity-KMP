package com.hamdy.clarity.consumer

import com.hamdy.clarity.ClarityClient
import com.hamdy.clarity.ClarityConfig
import com.hamdy.clarity.createClarityClient

fun iosConsumerSmoke(): ClarityClient = createClarityClient(ClarityConfig("consumer-test"))
