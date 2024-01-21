package net.kvibews.controller

import net.kvibews.model.DocumentState
import net.kvibews.dto.DocumentDTO
import net.kvibews.service.DocumentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/document")
class DocumentController(val documentService: DocumentService) {

    @PostMapping
    fun createDoc(@RequestBody document: DocumentDTO.Create): ResponseEntity<DocumentState> {
        return ResponseEntity.ok(documentService.createDocument(document))
    }

    @GetMapping("/{id}")
    fun getDoc(@PathVariable id: String): ResponseEntity<DocumentState> {
        return ResponseEntity.ok(documentService.getDocument(id))
    }

}