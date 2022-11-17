package fr.abes.qualimarc.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.qualimarc.core.service.NoticeService;
import fr.abes.qualimarc.core.service.ReferenceService;
import fr.abes.qualimarc.core.service.RuleService;
import fr.abes.qualimarc.core.utils.TypeAnalyse;
import fr.abes.qualimarc.core.utils.UtilsMapper;
import fr.abes.qualimarc.web.configuration.WebConfig;
import fr.abes.qualimarc.web.dto.PpnWithRuleSetsRequestDto;
import fr.abes.qualimarc.web.dto.ResultAnalyseResponseDto;
import fr.abes.qualimarc.web.dto.ResultRulesResponseDto;
import fr.abes.qualimarc.web.exception.ExceptionControllerHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {RuleController.class}) //  Active le Model-View-Controller, nécessaire pour éviter le code d'erreur 415 lors du lancement du test checkPpn
@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = {WebConfig.class})
public class RuleControllerTest {
    @Autowired
    WebApplicationContext context;

    @InjectMocks
    RuleController ruleController;

    @Autowired
    MappingJackson2HttpMessageConverter yamlHttpConverter;

    @Autowired
    MappingJackson2HttpMessageConverter jsonHttpConverter;

    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    RuleService ruleService;

    @MockBean
    NoticeService noticeService;

    @MockBean
    ReferenceService referenceService;

    @MockBean
    UtilsMapper utilsMapper;


    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(context.getBean(RuleController.class))
                .setMessageConverters(this.yamlHttpConverter, this.jsonHttpConverter)
                .setControllerAdvice(new ExceptionControllerHandler())
                .build();
    }

    @Test
    void checkPpn() throws Exception {
        //  Création de la liste de contrôle pour le Mockito
        ResultRulesResponseDto resultRulesResponseDto = new ResultRulesResponseDto();
        resultRulesResponseDto.setPpn("143519379");
        List<ResultRulesResponseDto> resultRulesResponseDtoList = new ArrayList<>();
        resultRulesResponseDtoList.add(resultRulesResponseDto);
        ResultAnalyseResponseDto resultAnalyseResponseDto = new ResultAnalyseResponseDto();
        resultAnalyseResponseDto.setResultRules(resultRulesResponseDtoList);
        //  Création du Mockito
        Mockito.when(utilsMapper.map(any(),any())).thenReturn(resultAnalyseResponseDto);

        //  Création de l'objet ControllingPpnWithRuleSetsRequestDto à passer dans la requête
        List<String> ppnList = new ArrayList<>();
        ppnList.add("143519379");
        TypeAnalyse typeAnalyse;
        typeAnalyse = TypeAnalyse.QUICK;
        PpnWithRuleSetsRequestDto ppnWithRuleSetsRequestDto = new PpnWithRuleSetsRequestDto();
        ppnWithRuleSetsRequestDto.setPpnList(ppnList);
        ppnWithRuleSetsRequestDto.setTypeAnalyse(typeAnalyse);
        String jsonRequest = objectMapper.writeValueAsString(ppnWithRuleSetsRequestDto);

        //  Appel et contrôle de la méthode
        this.mockMvc.perform(post("/api/v1/check")
                        .accept(MediaType.APPLICATION_JSON_VALUE).characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON_VALUE).characterEncoding(StandardCharsets.UTF_8)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultRules[0].ppn").value("143519379"));
    }

    @Test
    void testIndexRules() throws Exception {
        String yaml =
                "rules:\n" +
                "    - id:          2\n" +
                "      id-excel:    2\n" +
                "      type:        presencezone\n" +
                "      message:     message test 2\n" +
                "      zone:        330\n" +
                "      priorite:    P2\n" +
                "      presence:    false\n";

        this.mockMvc.perform(post("/api/v1/indexRules")
                .contentType("text/yml").characterEncoding(StandardCharsets.UTF_8)
                .content(yaml).characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("test création règle simple de type dependance")
    void testIndexRuleDependency() throws Exception {
        String yaml =
                "rules:\n" +
                        "    - id:          2\n" +
                        "      id-excel:    2\n" +
                        "      type:        dependance\n" +
                        "      zone:        330\n" +
                        "      souszone:    a";

        this.mockMvc.perform(post("/api/v1/indexRules")
                .contentType("text/yml").characterEncoding(StandardCharsets.UTF_8)
                .content(yaml).characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest())
                .andExpect(result -> result.getResponse().getContentAsString().contains("debugMessage: Une règle simple ne peut pas être une règle de dépendance"));
    }

    @Test
    @DisplayName("test création règle simple de type reciprocite")
    void testIndexRuleReciprocite() throws Exception {
        String yaml =
                "rules:\n" +
                        "    - id:          2\n" +
                        "      id-excel:    2\n" +
                        "      type:        reciprocite\n" +
                        "      zone:        330\n" +
                        "      souszone:    a";

        this.mockMvc.perform(post("/api/v1/indexRules")
                .contentType("text/yml").characterEncoding(StandardCharsets.UTF_8)
                .content(yaml).characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest())
                .andExpect(result -> result.getResponse().getContentAsString().contains("debugMessage: Une règle simple ne peut pas être de type reciprocite"));
    }

    @Test
    void testIndexCompleRule() throws Exception {
        String yaml =
                "---\n" +
                "rules:\n" +
                "   - id: 2\n" +
                "     id-excel: 2\n" +
                "     message: test\n" +
                "     priorite: P2\n" +
                "     type-doc:\n" +
                "       - A\n" +
                "       - O\n" +
                "     regles:\n" +
                "       - id: 2\n" +
                "         type: presencezone\n" +
                "         zone: '330'\n" +
                "         presence: false\n" +
                "       - id: 3\n" +
                "         type: presencezone\n" +
                "         zone: '330'\n" +
                "         presence: true\n" +
                "         operateur-booleen: ET\n";




        this.mockMvc.perform(post("/api/v1/indexComplexRules")
                .contentType("text/yml").characterEncoding(StandardCharsets.UTF_8)
                .content(yaml).characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk());
    }
//
//    @Test
//    @DisplayName("test l'indexation de règles complexes avec un opérateur manquant")
//    void testIndexCompleRuleWithoutOperateur() throws Exception {
//        String yaml =
//                "---\n" +
//                "rules:\n" +
//                "   - id: 2\n" +
//                "     id-excel: 2\n" +
//                "     message: test\n" +
//                "     priorite: P2\n" +
//                "     type-doc:\n" +
//                "       - A\n" +
//                "       - O\n" +
//                "     regles:\n" +
//                "       - id: 2\n" +
//                "         type: presencezone\n" +
//                "         zone: '330'\n" +
//                "         presence: false\n" +
//                "       - id: 3\n" +
//                "         type: presencezone\n" +
//                "         zone: '330'\n" +
//                "         presence: true\n";
//
//        Mockito.when(utilsMapper.map(Mockito.any(),ComplexRule.class)).thenThrow(new MappingException("Toute les regles"));
//
//        this.mockMvc.perform(post("/api/v1/indexComplexRules")
//                .contentType("text/yml").characterEncoding(StandardCharsets.UTF_8)
//                .content(yaml).characterEncoding(StandardCharsets.UTF_8))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.debugMessage").value("La première règle doit contenir un opérateur"));
//    }
//
//    @Test
//    @DisplayName("test handleComplexRulesWebDto")
//    void testHandleComplexRuleWebDto() throws Exception {
//        String yaml =
//                "---\n" +
//                "rules:\n" +
//                "   - id: 2\n" +
//                "     id-excel: 2\n" +
//                "     message: test\n" +
//                "     priorite: P2\n" +
//                "     type-doc:\n" +
//                "       - A\n" +
//                "       - O\n" +
//                "     regles:\n" +
//                "       - id: 2\n" +
//                "         type: presencezone\n" +
//                "         zone: '330'\n" +
//                "         presence: false\n";
//
//        Mockito.doNothing().when(ruleService).saveAll(Mockito.any());
//
//        this.mockMvc.perform(post("/api/v1/indexComplexRules")
//                .contentType("text/yml").characterEncoding(StandardCharsets.UTF_8)
//                .content(yaml).characterEncoding(StandardCharsets.UTF_8))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.debugMessage").value("Une règle complexe doit contenir au moins deux règles simples"));
//    }
}