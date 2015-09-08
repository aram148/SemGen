package semsim.annotation;

import java.net.URI;

public interface ReferenceTerm {
	
	/**
	 * Retrieve the first {@link ReferenceOntologyAnnotation} found applied to this object
	 * that uses the SemSim:hasPhysicalDefinition relation (SemSimConstants.HAS_PHYSICAL_DEFINITION_RELATION).
	 */
	public ReferenceOntologyAnnotation getPhysicalDefinitionReferenceOntologyAnnotation();
	
	/**
	 * Retrieve the reference URI.
	 */
	public URI getPhysicalDefinitionURI();
	
	/**
	 * @return True if an object has at least one {@link ReferenceOntologyAnnotation}, otherwise false;
	 */
	public Boolean hasPhysicalDefinitionAnnotation();
	
	public String getName();
	
	public String getNamewithOntologyAbreviation();
	
	public String getOntologyName();
	
	public String getTermID();
	
	public String getDescription();
}
