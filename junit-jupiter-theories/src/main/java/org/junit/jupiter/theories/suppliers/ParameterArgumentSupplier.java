package org.junit.jupiter.theories.suppliers;

import org.junit.jupiter.theories.domain.DataPointDetails;
import org.junit.jupiter.theories.domain.TheoryParameterDetails;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Interface for parameter argument suppliers. Implementations of this interface are used to turn parameter argument supplier annotations (annotations that have the
 * {@link org.junit.jupiter.theories.annotations.suppliers.ParametersSuppliedBy} meta-annotation) into lists of {@link DataPointDetails}.
 */
public interface ParameterArgumentSupplier {
    /**
     * Converts the provided parameter details and annotation into a list of {@link DataPointDetails}.
     *
     * @param parameterDetails the details of the parameter that will receive the constructed values
     * @param annotationToParse the annotation that contains the configuration for the arguments
     * @return the constructed arguments
     */
    List<DataPointDetails> buildArgumentsFromSupplierAnnotation(TheoryParameterDetails parameterDetails, Annotation annotationToParse);
}
