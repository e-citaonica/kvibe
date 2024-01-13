package net.kvibews.controller

import net.kvibews.model.Document
import net.kvibews.dto.DocumentCreationModel
import net.kvibews.service.DocumentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/doc")
class DocumentController(val documentService: DocumentService) {

    @PostMapping
    fun createDoc(@RequestBody document: DocumentCreationModel): ResponseEntity<Document> {
        return ResponseEntity.ok(documentService.createDocument(document))
    }

    @GetMapping("/{id}")
    fun getDoc(@PathVariable id: String): Document {
        return documentService.getDocument(id)
    }
}