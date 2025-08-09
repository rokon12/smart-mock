package ca.bazlur.smartmock.controller;

import ca.bazlur.smartmock.service.MockService;
import ca.bazlur.smartmock.model.MockResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/mock")
@RequiredArgsConstructor
public class MockController {
    private final MockService mockService;

    @RequestMapping("/**")
    public ResponseEntity<String> handleMockRequest(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        
        String path = request.getRequestURI().replace("/mock", "");
        String method = request.getMethod();
        log.debug("Handling mock request: {} {}", method, path);
        
        try {
            MockResult result = mockService.generate(request, body != null ? body : "");
            
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(result.getStatus());
            if (result.getHeaders() != null) {
                result.getHeaders().forEach((key, values) -> 
                    values.forEach(value -> responseBuilder.header(key, value)));
            }
            
            log.debug("Mock response generated with status: {}", result.getStatus());
            return responseBuilder.body(result.getBody());
            
        } catch (Exception e) {
            log.error("Error generating mock response", e);
            throw e;
        }
    }
}