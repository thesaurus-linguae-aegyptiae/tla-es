package tla.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {App.class})
@AutoConfigureMockMvc
class AppTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void app() {
        App classUnderTest = new App();
        assertTrue(
            classUnderTest.getClass().isAnnotationPresent(
                EnableAutoConfiguration.class
            ),
            "auto conf annotation should be present"
        );
    }

    @Test
    void http() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().is(404));
    }

}
