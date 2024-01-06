package net.kvibews.controller

import net.kvibews.model.Document
import net.kvibews.model.dto.DocumentCreationModel
import net.kvibews.service.DocumentService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/doc")
class DocumentController(val documentService: DocumentService) {

    @PostMapping
    fun createDoc(@RequestBody document: DocumentCreationModel): Document {
        return documentService.createDocument(document)
    }

    @GetMapping("/{documentId}")
    fun getDoc(@PathVariable documentId: String): Document? {
        return documentService.getDocument(documentId)
    }
}