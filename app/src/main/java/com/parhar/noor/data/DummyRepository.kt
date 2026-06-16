package com.parhar.noor.data

import com.parhar.noor.network.Resource
import kotlinx.coroutines.delay

class DummyRepository {

    suspend fun getWelcomeMessage(): Resource<String> {
        delay(300)
        return Resource.Success("Noor foundation is ready.")
    }
}
