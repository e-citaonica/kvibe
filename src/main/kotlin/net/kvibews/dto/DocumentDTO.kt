package net.kvibews.dto


object DocumentDTO {

    data class Create(
        val name: String,
        val language: String,
        val username: String
    )
}