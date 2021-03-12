package com.codex.sq2cql;

import com.codex.sq2cql.model.Mapping;
import com.codex.sq2cql.model.MappingContext;
import com.codex.sq2cql.model.common.TermCode;
import com.codex.sq2cql.model.cql.Library;
import com.codex.sq2cql.model.structured_query.ConceptCriterion;
import com.codex.sq2cql.model.structured_query.Criterion;
import com.codex.sq2cql.model.structured_query.NumericCriterion;
import com.codex.sq2cql.model.structured_query.StructuredQuery;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.codex.sq2cql.PrintContext.ZERO;
import static com.codex.sq2cql.model.common.Comparator.LESS_THAN;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexander Kiel
 */
class TranslatorTest {

    public static final TermCode NEOPLASM = TermCode.of("http://fhir.de/CodeSystem/dimdi/icd-10-gm", "C71",
            "Malignant neoplasm of brain");
    public static final TermCode PLATELETS = TermCode.of("http://loinc.org", "26515-7", "Platelets");
    public static final TermCode HYPERTENSION = TermCode.of("http://fhir.de/CodeSystem/dimdi/icd-10-gm", "I10",
            "Essential (Primary) Hypertension");
    public static final TermCode SERUM = TermCode.of("https://fhir.bbmri.de/CodeSystem/SampleMaterialType", "Serum",
            "Serum");
    public static final TermCode TMZ = TermCode.of("http://fhir.de/CodeSystem/dimdi/atc", "L01AX03",
            "Temozolomide");
    public static final TermCode LIPID = TermCode.of("http://fhir.de/CodeSystem/dimdi/atc", "C10AA",
            "lipid lowering drugs");

    private final static Map<String, String> CODE_SYSTEM_ALIASES = Map.of(
            "http://fhir.de/CodeSystem/dimdi/icd-10-gm", "icd10",
            "http://loinc.org", "loinc",
            "https://fhir.bbmri.de/CodeSystem/SampleMaterialType", "sample",
            "http://fhir.de/CodeSystem/dimdi/atc", "atc",
            "http://snomed.info/sct", "snomed",
            "http://hl7.org/fhir/administrative-gender", "gender");

    @Test
    void toCQL_Inclusion_OneDisjunctionWithOneCriterion() {
        Library library = Translator.of().toCql(StructuredQuery.of(
                List.of(List.of(Criterion.TRUE))));

        assertEquals("true", library.getExpressionDefinitions().get(0).getExpression().print(ZERO));
    }

    @Test
    void toCQL_Inclusion_OneDisjunctionWithTwoCriteria() {
        Library library = Translator.of().toCql(StructuredQuery.of(
                List.of(List.of(Criterion.TRUE, Criterion.FALSE))));

        assertEquals("(true) or\n(false)", library.getExpressionDefinitions().get(0).getExpression()
                .print(ZERO));
    }

    @Test
    void toCQL_Inclusion_TwoDisjunctionsWithOneCriterionEach() {
        Library library = Translator.of().toCql(StructuredQuery.of(
                List.of(List.of(Criterion.TRUE), List.of(Criterion.FALSE))));

        assertEquals("(true) and\n(false)", library.getExpressionDefinitions().get(0).getExpression().print(ZERO));
    }

    @Test
    void toCQL_Inclusion_TwoDisjunctionsWithTwoCriterionEach() {
        Library library = Translator.of().toCql(StructuredQuery.of(
                List.of(List.of(Criterion.TRUE, Criterion.TRUE), List.of(Criterion.FALSE, Criterion.FALSE))));

        assertEquals("((true) or\n(true)) and\n((false) or\n(false))", library.getExpressionDefinitions().get(0)
                .getExpression().print(ZERO));
    }

    @Test
    void toCQL_Inclusion_And_Exclusion_OneConjunctionWithOneCriterion() {
        Library library = Translator.of().toCql(StructuredQuery.of(
                List.of(List.of(Criterion.TRUE)),
                List.of(List.of(Criterion.FALSE))));

        assertEquals("define Inclusion:\n  true", library.getExpressionDefinitions().get(0).print(ZERO));
        assertEquals("define Exclusion:\n  false", library.getExpressionDefinitions().get(1).print(ZERO));
    }

    @Test
    void toCQL_Inclusion_And_Exclusion_OneConjunctionWithTwoCriteria() {
        Library library = Translator.of().toCql(StructuredQuery.of(
                List.of(List.of(Criterion.TRUE)),
                List.of(List.of(Criterion.TRUE, Criterion.FALSE))));

        assertEquals("define Exclusion:\n  (true) and\n  (false)", library.getExpressionDefinitions().get(1)
                .print(ZERO));
    }

    @Test
    void toCQL_Inclusion_And_Exclusion_TwoConjunctionsWithOneCriterionEach() {
        Library library = Translator.of().toCql(StructuredQuery.of(
                List.of(List.of(Criterion.TRUE)),
                List.of(List.of(Criterion.TRUE), List.of(Criterion.FALSE))));

        assertEquals("(true) or\n(false)", library.getExpressionDefinitions().get(1).getExpression()
                .print(ZERO));
    }

    @Test
    void toCQL_Inclusion_And_Exclusion_TwoConjunctionsWithTwoCriterionEach() {
        Library library = Translator.of().toCql(StructuredQuery.of(
                List.of(List.of(Criterion.TRUE)),
                List.of(List.of(Criterion.TRUE, Criterion.TRUE), List.of(Criterion.FALSE, Criterion.FALSE))));

        assertEquals("((true) and\n(true)) or\n((false) and\n(false))", library.getExpressionDefinitions().get(1)
                .getExpression().print(ZERO));
    }

    @Test
    void toCQL_Usage_Documentation() {
        var neoplasm = TermCode.of("http://fhir.de/CodeSystem/dimdi/icd-10-gm", "C71", "Malignant neoplasm of brain");
        var codeSystemAliases = Map.of("http://fhir.de/CodeSystem/dimdi/icd-10-gm", "icd10");
        var mappingContext = MappingContext.of(Map.of(neoplasm, Mapping.of(neoplasm, "Condition")), codeSystemAliases);

        Library library = Translator.of(mappingContext).toCql(StructuredQuery.of(List.of(
                List.of(ConceptCriterion.of(neoplasm)))));

        assertEquals("""
                library Retrieve
                using FHIR version '4.0.0'
                include FHIRHelpers version '4.0.0'
                                                   
                codesystem icd10: 'http://fhir.de/CodeSystem/dimdi/icd-10-gm'                
                                
                define InInitialPopulation:
                  exists([Condition: Code 'C71' from icd10])
                """, library.print(ZERO));
    }

    @Test
    void toCQL_Test_Task1() {
        var mappingContext = MappingContext.of(Map.of(PLATELETS, Mapping.of(PLATELETS, "Observation"),
                NEOPLASM, Mapping.of(NEOPLASM, "Condition"),
                TMZ, Mapping.of(TMZ, "MedicationStatement")),
                CODE_SYSTEM_ALIASES);

        Library library = Translator.of(mappingContext).toCql(StructuredQuery.of(List.of(
                List.of(ConceptCriterion.of(NEOPLASM)),
                List.of(NumericCriterion.of(PLATELETS, LESS_THAN, BigDecimal.valueOf(50), "g/dl")),
                List.of(ConceptCriterion.of(TMZ)))));

        assertEquals("""
                library Retrieve
                using FHIR version '4.0.0'
                include FHIRHelpers version '4.0.0'
                                                   
                codesystem atc: 'http://fhir.de/CodeSystem/dimdi/atc'                
                codesystem icd10: 'http://fhir.de/CodeSystem/dimdi/icd-10-gm'                
                codesystem loinc: 'http://loinc.org'
                                
                define InInitialPopulation:
                  (exists([Condition: Code 'C71' from icd10])) and 
                  (exists(from [Observation: Code '26515-7' from loinc] O
                    where (O.value as Quantity) < (50 'g/dl'))) and
                  (exists([MedicationStatement: Code 'L01AX03' from atc]))
                """, library.print(ZERO));
    }

    @Test
    void toCQL_Test_Task2() {
        var mappingContext = MappingContext.of(Map.of(PLATELETS, Mapping.of(PLATELETS, "Observation"),
                HYPERTENSION, Mapping.of(HYPERTENSION, "Condition"),
                SERUM, Mapping.of(SERUM, "Specimen"),
                LIPID, Mapping.of(LIPID, "MedicationStatement")),
                CODE_SYSTEM_ALIASES);

        Library library = Translator.of(mappingContext).toCql(StructuredQuery.of(List.of(
                List.of(ConceptCriterion.of(HYPERTENSION)),
                List.of(ConceptCriterion.of(SERUM))), List.of(
                List.of(ConceptCriterion.of(LIPID)))));

        assertEquals("""
                library Retrieve
                using FHIR version '4.0.0'
                include FHIRHelpers version '4.0.0'
                                         
                codesystem atc: 'http://fhir.de/CodeSystem/dimdi/atc'          
                codesystem icd10: 'http://fhir.de/CodeSystem/dimdi/icd-10-gm'                
                codesystem sample: 'https://fhir.bbmri.de/CodeSystem/SampleMaterialType'                
                                
                define Inclusion:
                  (exists([Condition: Code 'I10' from icd10])) and 
                  (exists([Specimen: Code 'Serum' from sample]))
                                
                define Exclusion:
                  exists([MedicationStatement: Code 'C10AA' from atc])                
                                
                define InInitialPopulation:
                  (Inclusion) and 
                  (not (Exclusion))
                """, library.print(ZERO));
    }
}
