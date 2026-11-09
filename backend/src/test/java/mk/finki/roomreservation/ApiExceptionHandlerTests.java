package mk.finki.roomreservation;

import mk.finki.roomreservation.exception.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void invalidArgumentReturns400() throws Exception {
        mockMvc.perform(get("/test/errors/invalid-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Room ID must be greater than 0."));
    }

    @Test
    void invalidDateReturns400() throws Exception {
        mockMvc.perform(get("/test/errors/invalid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid date or time format."));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/test/errors/invalid-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid JSON request body."));
    }

    @Test
    void missingResourceReturns404() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Requested resource was not found."));
    }

    @Test
    void databaseConstraintReturns409WithoutSqlDetails() throws Exception {
        mockMvc.perform(get("/test/errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("The request conflicts with existing data or database constraints."));
    }

    @Test
    void unavailableDatabaseReturns503() throws Exception {
        mockMvc.perform(get("/test/errors/database-unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Database service is temporarily unavailable."));
    }

    @Test
    void unexpectedDatabaseErrorReturns500() throws Exception {
        mockMvc.perform(get("/test/errors/database-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("An internal database error occurred."));
    }

    @Test
    void accessDeniedReturns403() throws Exception {
        mockMvc.perform(get("/test/errors/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access denied."));
    }

    @Test
    void unexpectedRuntimeErrorReturns500WithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/test/errors/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("An unexpected server error occurred."));
    }

    @RestController
    @RequestMapping("/test/errors")
    static class TestController {

        @GetMapping("/invalid-argument")
        public void invalidArgument() {
            throw new IllegalArgumentException(
                    "Room ID must be greater than 0."
            );
        }

        @GetMapping("/invalid-date")
        public void invalidDate() {
            LocalDate.parse("invalid-date");
        }

        @PostMapping("/invalid-json")
        public Map<String, Object> invalidJson(
                @RequestBody Map<String, Object> body
        ) {
            return body;
        }

        @GetMapping("/not-found")
        public void notFound() {
            throw new EmptyResultDataAccessException(1);
        }

        @GetMapping("/conflict")
        public void conflict() {
            throw new DataIntegrityViolationException(
                    "Sensitive PostgreSQL constraint details"
            );
        }

        @GetMapping("/database-unavailable")
        public void databaseUnavailable() {
            throw new DataAccessResourceFailureException(
                    "Sensitive database connection information"
            );
        }

        @GetMapping("/database-error")
        public void databaseError() {
            throw new InvalidDataAccessResourceUsageException(
                    "Sensitive SQL query details"
            );
        }

        @GetMapping("/forbidden")
        public void forbidden() {
            throw new AccessDeniedException(
                    "Access denied"
            );
        }

        @GetMapping("/runtime")
        public void runtime() {
            throw new IllegalStateException(
                    "Sensitive internal application details"
            );
        }
    }
}