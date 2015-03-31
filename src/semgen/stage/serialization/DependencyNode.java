package semgen.stage.serialization;

import java.util.ArrayList;

import semgen.SemGen;
import semsim.model.computational.datastructures.DataStructure;

/**
 * Represents a dependency node in the d3 graph
 * 
 * @author Ryan
 *
 */
public class DependencyNode extends Node {	
	
	public String nodeType;
	public ArrayList<Object> inputs;
	
	public DependencyNode(DataStructure dataStructure)
	{
		this(dataStructure.getName(), dataStructure);
	}
	
	/**
	 * Allow descendant classes to pass in a node name
	 * @param name name of node
	 * @param dataStructure node data
	 */
	protected DependencyNode(String name, DataStructure dataStructure)
	{
		super(name);
		
		this.nodeType = dataStructure.getPropertyType(SemGen.semsimlib).toString();

		inputs = new ArrayList<Object>();

		// Are there intra-model inputs?
		if(dataStructure.getComputation() != null) {
			for(DataStructure input : dataStructure.getComputation().getInputs())
			{
				this.inputs.add(getName(input));
			}
		}
	}

	/**
	 * Get the data structure's name
	 * @param dataStructure
	 * @return the data structure's name
	 */
	protected String getName(DataStructure dataStructure) {
		return dataStructure.getName();
	}
}
