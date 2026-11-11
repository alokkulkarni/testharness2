package app.todo.common;

import static org.assertj.core.api.Assertions.assertThat;

import app.todo.testsupport.Contracts;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemCodeTest {

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("NFR-004 AC-2 the values of ProblemCode equal the Problem.code enum of common-problem-v1.yaml (ADR-0011)")
    void NFR_004_AC2_problem_code_values_equal_the_contract_enum() {
        Map<String, Object> doc = Contracts.yaml(Contracts.commonFile());
        Map<String, Object> components = (Map<String, Object>) doc.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        Map<String, Object> problem = (Map<String, Object>) schemas.get("Problem");
        Map<String, Object> properties = (Map<String, Object>) problem.get("properties");
        Map<String, Object> code = (Map<String, Object>) properties.get("code");
        List<String> contractCodes = (List<String>) code.get("enum");

        assertThat(contractCodes).as("the contract's code list is read").hasSize(11);
        assertThat(Arrays.stream(ProblemCode.values()).map(Enum::name).toList())
                .containsExactlyInAnyOrderElementsOf(contractCodes);
    }
}
