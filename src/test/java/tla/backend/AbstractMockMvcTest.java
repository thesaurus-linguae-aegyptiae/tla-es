package tla.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Brings an auto-configured {@link MockMvc} object.
 */
@SpringBootTest(classes = {App.class})
@AutoConfigureMockMvc
public abstract class AbstractMockMvcTest {

    @Autowired
    protected MockMvc mockMvc;

}