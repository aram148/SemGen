package semgen.annotation.composites;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.semanticweb.owlapi.model.OWLException;

import semgen.ExternalURLButton;
import semgen.SemGenGUI;
import semgen.annotation.AnnotationDialog;
import semgen.annotation.CustomPhysicalComponentEditor;
import semsim.Annotatable;
import semsim.SemSimConstants;
import semsim.model.SemSimComponent;
import semsim.model.SemSimModel;
import semsim.model.computational.DataStructure;
import semsim.model.annotation.ReferenceOntologyAnnotation;
import semsim.model.physical.CompositePhysicalEntity;
import semsim.model.physical.CustomPhysicalEntity;
import semsim.model.physical.CustomPhysicalProcess;
import semsim.model.physical.PhysicalEntity;
import semsim.model.physical.PhysicalModelComponent;
import semsim.model.physical.PhysicalProcess;
import semsim.model.physical.PhysicalProperty;
import semsim.model.physical.Submodel;
import semsim.writing.CaseInsensitiveComparator;

public class SemSimComponentAnnotationPanel extends JPanel implements MouseListener, ActionListener{

	private static final long serialVersionUID = -6606265105415658572L;
	public Annotatable smc;
	public AnnotationDialog anndialog;
	public SemSimModel semsimmodel;
	public JComboBox<String> combobox;
	public JLabel searchlabel = new JLabel(SemGenGUI.searchicon);
	public JLabel createlabel = new JLabel(SemGenGUI.createicon);
	public ExternalURLButton urlbutton = new ExternalURLButton();
	public JLabel eraselabel = new JLabel(SemGenGUI.eraseicon);
	public JLabel modifylabel = new JLabel(SemGenGUI.modifyicon);
	public Map<String,SemSimComponent> listdataandsmcmap = new HashMap<String, SemSimComponent>();
	public Object selecteditem;
	public Boolean editable;
	
	public SemSimComponentAnnotationPanel(AnnotationDialog anndia, Annotatable smc){
		this.anndialog = anndia;
		this.smc = smc;
		this.semsimmodel = anndia.semsimmodel;
		this.setBackground(new Color(207, 215, 252));
		
		editable = anndialog.thebutton.editable;
		
		combobox = new JComboBox<String>(new String[]{});
		combobox.setPreferredSize(new Dimension(350,30));
		combobox.setMaximumSize(new Dimension(350,30));
		combobox.setEnabled(editable);
		
		refreshComboBoxItemsAndButtonVisibility();
		
		if(smc.hasRefersToAnnotation())
			urlbutton.setTermURI(smc.getFirstRefersToReferenceOntologyAnnotation().getReferenceURI());
		
		searchlabel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		searchlabel.setBackground(Color.white);
		searchlabel.addMouseListener(this);
		searchlabel.setToolTipText("Look up reference ontology term");
		searchlabel.setEnabled(editable);
		createlabel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		createlabel.setBackground(Color.white);
		createlabel.addMouseListener(this);
		createlabel.setToolTipText("Create new custom term");
		createlabel.setEnabled(editable);
		eraselabel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		eraselabel.setBackground(Color.white);
		eraselabel.addMouseListener(this);
		eraselabel.setToolTipText("Remove annotation component");
		eraselabel.setEnabled(editable);
		modifylabel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		modifylabel.setBackground(Color.white);
		modifylabel.addMouseListener(this);
		modifylabel.setToolTipText("Edit custom term");
		modifylabel.setEnabled(editable);
		
		setLayout(new BorderLayout());
		JPanel itempanel = new JPanel();
		itempanel.setBackground(SemGenGUI.lightblue);
		itempanel.add(combobox);
		itempanel.add(urlbutton);
		itempanel.add(searchlabel);
		itempanel.add(createlabel);
		itempanel.add(modifylabel);
		itempanel.add(eraselabel);
		
		add(itempanel, BorderLayout.WEST);
		add(Box.createGlue(), BorderLayout.EAST);
	}
	
	
	public void refreshComboBoxItemsAndButtonVisibility(){
		
		ArrayList<String> stringlist = new ArrayList<String>();
		String[] stringarray;
		removeComboBoxActionListeners(this);
		combobox.removeAllItems();
		
		// If we're annotating a physical model component (i.e. a component in a composite physical entity)
		if(smc instanceof PhysicalModelComponent && !(smc instanceof Submodel)){ 
			for(PhysicalModelComponent pmctolist : anndialog.semsimmodel.getPhysicalModelComponents()){
				if(pmctolist.getName()!=null && !pmctolist.getName().equals("") && (
						((pmctolist instanceof PhysicalProperty) && (smc instanceof PhysicalProperty))
						|| ((pmctolist instanceof PhysicalEntity) && (smc instanceof PhysicalEntity))
						|| ((pmctolist instanceof PhysicalProcess) && (smc instanceof PhysicalProcess)))){
					
					String id = null;
					
					// Composite physical entities won't have refersTo Annotations
					if(pmctolist.hasRefersToAnnotation()){
						id = pmctolist.getFirstRefersToReferenceOntologyAnnotation().getValueDescription();
						id = id + " (" + pmctolist.getFirstRefersToReferenceOntologyAnnotation().getOntologyAbbreviation() + ")"; 
					}
					else if(pmctolist.getName()!=null && !(pmctolist instanceof PhysicalProperty)){
						if(pmctolist instanceof CompositePhysicalEntity){
							PhysicalModelComponent unspecpmc = anndialog.semsimmodel.getCustomPhysicalEntityByName(SemGenGUI.unspecifiedName);
							if(!((CompositePhysicalEntity)pmctolist).getArrayListOfEntities().contains(unspecpmc))
								id = pmctolist.getName();
						}
						else id = pmctolist.getName();
					}
	 				
					// If we have an id and this term hasn't been added to the list
					if(id!=null && !stringlist.contains(id)){
						
						// If the data structure is a property of a singular physical entity, make the
						// composite physical entities available for re-use
						if(pmctolist instanceof CompositePhysicalEntity){
							if(!(smc instanceof PhysicalProperty)){
								PhysicalModelComponent temp = ((DataStructure)anndialog.smc).getPhysicalProperty().getPhysicalPropertyOf();
								if(!(temp instanceof CompositePhysicalEntity)){
									stringlist.add(id);
								}
							}
						}
						else stringlist.add(id);
					}
					listdataandsmcmap.put(id, pmctolist);
				}
			}
		}
		// If we're annotating a DataStructure or Submodel
		else{
			Set<Annotatable> annotatables = new HashSet<Annotatable>();
			annotatables.addAll(anndialog.semsimmodel.getPhysicalModelComponents());
			annotatables.addAll(anndialog.semsimmodel.getDataStructures());
			
			for(Annotatable ann : annotatables){
				String id = null;
				if(ann.hasRefersToAnnotation()){
					id = ann.getFirstRefersToReferenceOntologyAnnotation().getValueDescription();
					id = id + " (" + ann.getFirstRefersToReferenceOntologyAnnotation().getOntologyAbbreviation() + ")"; 
					if(id!=null && !stringlist.contains(id)){
						stringlist.add(id);
						listdataandsmcmap.put(id, (SemSimComponent) ann);
					}
				}
			}
		}
		
		Collections.sort(stringlist, new CaseInsensitiveComparator());

		// Allow the use of the *unspecified* option for select types of Annotatable structures
		if(smc instanceof PhysicalProperty || smc instanceof DataStructure || smc instanceof Submodel){
			stringlist.add(0,SemGenGUI.unspecifiedName);
			listdataandsmcmap.put(SemGenGUI.unspecifiedName, null);
		}
		else if(smc instanceof PhysicalProcess) combobox.setFont(new Font("SansSerif", Font.ITALIC, SemGenGUI.defaultfontsize));
		
		stringarray = stringlist.toArray(new String[]{});
		String text = null;
		
		// If annotation present, select it in the combo box...
		if(smc.hasRefersToAnnotation()){
			ReferenceOntologyAnnotation refann = smc.getFirstRefersToReferenceOntologyAnnotation();
			text = refann.getValueDescription() + " (" + refann.getOntologyAbbreviation() + ")";
		}
		// ...otherwise select the unspecified option or the SemSimComponent's name, in the case of CustomPhysical_ components
		else{
			if(smc instanceof PhysicalProperty || smc instanceof DataStructure || smc instanceof Submodel)
				text = SemGenGUI.unspecifiedName;
			else text = ((SemSimComponent)smc).getName();
		}
		combobox.setModel(new DefaultComboBoxModel<String>(stringarray));
		combobox.setSelectedItem(text);
		combobox.validate();
		selecteditem = combobox.getSelectedItem();
		combobox.setToolTipText((String)combobox.getSelectedItem());
		combobox.addActionListener(this);
		updateButtonVisibility();
	}
	
	public void updateButtonVisibility(){
		if(smc instanceof PhysicalProperty || smc instanceof DataStructure || smc instanceof Submodel) createlabel.setVisible(false);
		else createlabel.setVisible(true);
		
		if(smc instanceof CustomPhysicalEntity || smc instanceof CustomPhysicalProcess){
			PhysicalModelComponent pmc = (PhysicalModelComponent)smc;
			if(!pmc.getName().equals(SemGenGUI.unspecifiedName)){
				modifylabel.setVisible(true);
				urlbutton.setVisible(false);
			}
		}
		else{
			modifylabel.setVisible(false);
			urlbutton.setVisible(true);
		}
		if(combobox.getSelectedItem().equals(SemGenGUI.unspecifiedName)){
			modifylabel.setVisible(false);
			urlbutton.setVisible(false);
		}
		if(smc instanceof PhysicalProcess) searchlabel.setVisible(false);
	}
	

	public void mouseEntered(MouseEvent e) {
		Component component = e.getComponent();
		if (component instanceof JLabel) {
			((JLabel)component).setCursor(new Cursor(Cursor.HAND_CURSOR));
		}
	}

	public void mouseExited(MouseEvent e) {
		Component component = e.getComponent();
		if (component instanceof JLabel) {
			((JLabel)component).setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void mouseClicked(MouseEvent arg0) {
		Component clickedcomponent = arg0.getComponent();
		
		// If we're opening the search dialog for ontology terms
		if (clickedcomponent == searchlabel) {
			String[] ontList = null;
			if(smc instanceof PhysicalModelComponent && !(smc instanceof Submodel)){
				if(smc instanceof PhysicalProperty)
					ontList = new String[]{SemSimConstants.ONTOLOGY_OF_PHYSICS_FOR_BIOLOGY_FULLNAME};
				else if(smc instanceof PhysicalEntity){
					ontList = new String[]{
							SemSimConstants.CELL_TYPE_ONTOLOGY_FULLNAME,
							SemSimConstants.CHEMICAL_ENTITIES_OF_BIOLOGICAL_INTEREST_FULLNAME,
							SemSimConstants.FOUNDATIONAL_MODEL_OF_ANATOMY_FULLNAME,
							SemSimConstants.GENE_ONTOLOGY_FULLNAME,
							SemSimConstants.MOUSE_ADULT_GROSS_ANATOMY_ONTOLOGY_FULLNAME,
							SemSimConstants.UNIPROT_FULLNAME};
				}
				else if(smc instanceof PhysicalProcess)
					ontList = new String[]{SemSimConstants.GENE_ONTOLOGY_FULLNAME};
				new CompositeAnnotationComponentSearchDialog(this, ontList, new String[]{"Apply","Cancel"});
			}
			else{
				anndialog.showSingularAnnotationEditor();
			}
		}
		else if(clickedcomponent == createlabel && smc instanceof PhysicalModelComponent){
			Boolean proc = false;
			String porestring = "entity";
			if(smc instanceof PhysicalProcess){
				proc = true;
				porestring = "process";
			}
			String input = JOptionPane.showInputDialog("Enter name for new custom physical " + porestring);
			if(input!=null && !input.equals("")){
				if(proc) smc = semsimmodel.addCustomPhysicalProcess(input, "");
				else smc = semsimmodel.addCustomPhysicalEntity(input, "");
				
				try {
					anndialog.updateCompositeAnnotationFromUIComponents();
				} catch (OWLException e) {
					e.printStackTrace();
				}
				
				for(Component c : anndialog.compositepanel.getComponents()){
					if(c instanceof SemSimComponentAnnotationPanel){
						SemSimComponentAnnotationPanel pan = (SemSimComponentAnnotationPanel)c;
						removeComboBoxActionListeners(pan);
						pan.refreshComboBoxItemsAndButtonVisibility();
						pan.combobox.addActionListener(pan);
					}
				}
				new CustomPhysicalComponentEditor(anndialog, (PhysicalModelComponent)smc);
			}
		}
		else if(clickedcomponent == modifylabel && smc instanceof PhysicalModelComponent){
			anndialog.annotator.customtermeditor = new CustomPhysicalComponentEditor(anndialog, (PhysicalModelComponent)smc);
		}
		// If we're removing a component of the annotation
		else if(clickedcomponent == eraselabel){
			
			// If we're erasing a component in a composite annotation...
			if(smc instanceof PhysicalModelComponent && !(smc instanceof Submodel)){
				int componentindex = 0;
				Component[] comps = anndialog.compositepanel.getComponents();
				for(int c=0; c<comps.length; c++){
					if(comps[c] == this) componentindex = c;
					if(comps[c] instanceof SemSimComponentAnnotationPanel) {
					}
				}
				// If we're removing a property annotation, just remove reference anns from property and refresh
				if(componentindex==0){
					removeAsPhysicalPropertyAnnotation();
					// If only the physical property part of the composite is left, and we're removing it, set the 
					// data structure's physical property to null
				}
				// Otherwise, actually remove the physicalmodelcomponentpanel and structuralrelationpanel, if present
				else{
					Component nextcomp = anndialog.compositepanel.getComponent(componentindex + 1);
					Component prevcomp = anndialog.compositepanel.getComponent(componentindex - 1);
					anndialog.compositepanel.remove(this);
					if(nextcomp instanceof StructuralRelationPanel) anndialog.compositepanel.remove(nextcomp);
					else if(prevcomp instanceof StructuralRelationPanel) anndialog.compositepanel.remove(prevcomp);
					try {
						anndialog.updateCompositeAnnotationFromUIComponents();
					} catch (OWLException e) {
						e.printStackTrace();
					}
				}
				anndialog.compositepanel.refreshUI();
			}
			// ...otherwise we're removing a singular annotation
			else removeAsSingularAnnotation();
		}
	}

	public void mousePressed(MouseEvent arg0) {
		Component component = arg0.getComponent();
		if (component instanceof JLabel) {
			((JLabel)component).setBorder(BorderFactory.createLineBorder(Color.blue,1));
		}
	}

	public void mouseReleased(MouseEvent arg0) {
		Component component = arg0.getComponent();
		if (component instanceof JLabel) {
			((JLabel)component).setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		}
	}

	public void actionPerformed(ActionEvent arg0) {
		Object o = arg0.getSource();
		if(o == combobox){
			
			// If for property, look up the pmc for the item, but don't change the pmc, just apply its annotation to the current pmc
			Annotatable selectedsmc = (Annotatable) listdataandsmcmap.get(combobox.getSelectedItem());
			if(smc instanceof PhysicalProperty){
				if(selectedsmc!=null){
					if(selectedsmc.hasRefersToAnnotation()){
						ReferenceOntologyAnnotation otherann = selectedsmc.getFirstRefersToReferenceOntologyAnnotation();
						
						// Check if OPB property is valid
						if(checkOPBpropertyValidity(otherann.getReferenceURI())){
							applyReferenceOntologyAnnotation(otherann, true);
						}
						else{
							combobox.setPopupVisible(false);
							showInvalidOPBpropertyError();
							combobox.setSelectedItem(selecteditem);
							return;
						}
					}
				}
				// Otherwise the "unspecified" option was selected for the physical property
				else removeAsPhysicalPropertyAnnotation();
				
				// Update the annotation button codes and the order of the button in the scrollpane
				boolean rescroll = anndialog.thebutton.refreshAllCodes();
				if(rescroll && SemGenGUI.annotateitemsortbytype.isSelected()){
					anndialog.annotator.AlphabetizeAndSetCodewords();
					SemGenGUI.scrollToComponent(anndialog.annotator.codewordpanel, anndialog.thebutton);
				}
			}
			// If not for a physical property, get the reference annotation for the selected item and apply it to whatever is being annotated
			else{
				if(selectedsmc!=null){
					// If we are re-using a composite physical entity, just make it the object of the physical property
					// and refresh the Annotation Dialog UI
					if(selectedsmc instanceof CompositePhysicalEntity){
						anndialog.compositepanel.datastructure.getPhysicalProperty().setPhysicalPropertyOf((CompositePhysicalEntity)selectedsmc);
						anndialog.compositepanel.refreshUI();
						return;
					}
					// This happens for singular annotations and non-composite physical entity annotations
					if(selectedsmc.hasRefersToAnnotation()){
						//more411label.termuri = pmc.getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString();
						urlbutton.setTermURI(selectedsmc.getFirstRefersToReferenceOntologyAnnotation().getReferenceURI());
					}
				}
				// ... otherwise the *unspecified* item was selected
				else if(smc instanceof DataStructure || smc instanceof Submodel){
					removeAsSingularAnnotation();
					return;
				}
			}
			// If we're annotating a DataStructure and this panel does not represent the data structure itself (it represents a component in a composite)
			if(anndialog.smc instanceof DataStructure && !(smc instanceof DataStructure)){
				// Update the physical model component represented in this panel to the new one selected by the user
				if(!(smc instanceof PhysicalProperty)) this.smc = selectedsmc;
				try {
					anndialog.updateCompositeAnnotationFromUIComponents();
				} catch (OWLException e) {
					e.printStackTrace();
				}
				anndialog.compositepanel.setAddButtonsEnabled();
			}
			// If we are editing either a DataStructure or Submodel's singular annotation
			else  applyReferenceOntologyAnnotation(selectedsmc.getFirstRefersToReferenceOntologyAnnotation(), true);
			
			refreshComboBoxItemsAndButtonVisibility();
		}
	}
	
	public boolean checkOPBpropertyValidity(URI OPBuri){
		boolean isvalid = true; 
		PhysicalProperty prop = (PhysicalProperty)smc;
		if(prop.getPhysicalPropertyOf()!=null){
			
			// This conditional statement makes sure that physical processes are annotated with appropriate OPB terms
			// It only limits physical entity properties to non-process properties. It does not limit based on whether
			// the OPB term is for a constitutive property. Not sure if it should, yet.
			if((prop.getPhysicalPropertyOf() instanceof PhysicalEntity && 
					(SemGenGUI.OPBflowProperties.contains(OPBuri.toString()) || SemGenGUI.OPBprocessProperties.contains(OPBuri.toString())))
					|| (prop.getPhysicalPropertyOf() instanceof PhysicalProcess &&
				(!SemGenGUI.OPBflowProperties.contains(OPBuri.toString()) && !SemGenGUI.OPBprocessProperties.contains(OPBuri.toString())))){
						isvalid = false;
			}
		}
		return isvalid;
	}
	
	public void showInvalidOPBpropertyError(){
		JOptionPane.showMessageDialog(SemGenGUI.desktop, "That physical property is not valid for the physical entity\n or process specified in this composite annotation.",
				"Invalid annotation", JOptionPane.ERROR_MESSAGE);
	}
	
	public void removeComboBoxActionListeners(SemSimComponentAnnotationPanel pan){
		ActionListener[] al = pan.combobox.getActionListeners();
		for(int x=0;x<al.length;x++){
			pan.combobox.removeActionListener(al[x]);
		}
	}
	
	public void applyReferenceOntologyAnnotation(ReferenceOntologyAnnotation ann, boolean refreshCodes){
		smc.removeAllReferenceAnnotations();
		smc.addAnnotation(ann);
		urlbutton.setTermURI(ann.getReferenceURI());
		anndialog.annotator.setModelSaved(false);
		if(refreshCodes) anndialog.thebutton.refreshAllCodes();
	}
	
	private void removeAsPhysicalPropertyAnnotation(){
		anndialog.compositepanel.datastructure.getPhysicalProperty().removeAllReferenceAnnotations();
		anndialog.annotator.setModelSaved(false);
		anndialog.thebutton.refreshAllCodes();
	}
	
	private void removeAsSingularAnnotation(){
		smc.removeAllReferenceAnnotations();
		anndialog.thebutton.refreshAllCodes();
		anndialog.annotator.setModelSaved(false);
		refreshComboBoxItemsAndButtonVisibility();
	}
}
