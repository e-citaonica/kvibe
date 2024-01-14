package net.kvibews.controller

import net.kvibews.model.DocumentState
import net.kvibews.dto.DocumentDTO
import net.kvibews.service.DocumentOperationHandlerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/doc")
class DocumentController(val documentOperationHandlerService: DocumentOperationHandlerService) {

    @PostMapping
    fun createDoc(@RequestBody document: DocumentDTO.Create): ResponseEntity<DocumentState> {
        return ResponseEntity.ok(documentOperationHandlerService.createDocument(document))
    }

    @GetMapping("/{id}")
    fun getDoc(@PathVariable id: String): DocumentState {
        return documentOperationHandlerService.getDocument(id)
    }
}