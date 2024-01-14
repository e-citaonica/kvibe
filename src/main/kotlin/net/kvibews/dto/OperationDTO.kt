package net.kvibews.dto

object OperationDTO {

    data class AckMessage(
        val revision: Int
    )
}