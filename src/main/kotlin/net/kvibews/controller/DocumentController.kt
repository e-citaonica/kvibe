package net.kvibews.controller

import net.kvibews.model.DocumentState
import net.kvibews.dto.DocumentDTO
import net.kvibews.service.OperationHandlerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/doc")
class DocumentController(val operationHandlerService: OperationHandlerService) {

    @PostMapping
    fun createDoc(@RequestBody document: DocumentDTO.Create): ResponseEntity<DocumentState> {
        return ResponseEntity.ok(operationHandlerService.createDocument(document))
    }

    @GetMapping("/{id}")
    fun getDoc(@PathVariable id: String): DocumentState {
        return operationHandlerService.getDocument(id)
    }
}