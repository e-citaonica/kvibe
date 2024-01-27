package net.kvibews.controller

import net.kvibews.model.DocumentState
import net.kvibews.dto.DocumentDTO
import net.kvibews.model.DocumentOverview
import net.kvibews.service.DocumentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/document")
class DocumentController(val documentService: DocumentService) {

    @GetMapping
    fun getDoc(): ResponseEntity<List<DocumentState>> {
        return ResponseEntity.ok(documentService.getAllDocuments())
    }

    @GetMapping("/overview")
    fun getDocumentOverviews(): ResponseEntity<List<DocumentOverview>> {
        val a = documentService.getDocumentOverviews()
        return ResponseEntity.ok(documentService.getDocumentOverviews())
    }

    @GetMapping("/overview/{id}")
    fun getDocumentOverview(@PathVariable id: String): ResponseEntity<DocumentOverview> {
        return ResponseEntity.ok(documentService.getDocumentOverview(id))
    }

    @GetMapping("/{id}")
    fun getDoc(@PathVariable id: String): ResponseEntity<DocumentState> {
        return ResponseEntity.ok(documentService.getDocument(id))
    }

    @PostMapping
    fun createDoc(@RequestBody document: DocumentDTO.Create): ResponseEntity<DocumentState> {
        return ResponseEntity.ok(documentService.createDocument(document))
    }

}