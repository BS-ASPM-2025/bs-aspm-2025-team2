package bsaspm2025team2.backend.api;

import bsaspm2025team2.backend.api.dto.UploadResumeResponse;
import bsaspm2025team2.backend.service.ResumeUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/hr/candidates")
public class ResumeUploadController {

    private final ResumeUploadService resumeUploadService;

    public ResumeUploadController(ResumeUploadService resumeUploadService) {
        this.resumeUploadService = resumeUploadService;
    }

    @PostMapping(value = "/upload-resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResumeResponse uploadResume(@RequestPart("file") MultipartFile file) {
        Long candidateId = resumeUploadService.uploadResume(file);
        return new UploadResumeResponse(candidateId, "Resume uploaded successfully");
    }
}
