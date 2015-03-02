package semgen.annotation.annotatorpane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.semanticweb.owlapi.model.OWLException;

import semgen.GlobalActions;
import semgen.SemGenSettings;
import semgen.annotation.AnnotatorTab;
import semgen.annotation.annotatorpane.composites.CompositeAnnotationPanel;
import semgen.annotation.annotatorpane.composites.SemSimComponentAnnotationPanel;
import semgen.annotation.annotatorpane.composites.StructuralRelationPanel;
import semgen.annotation.componentlistpanes.AnnotationObjectButton;
import semgen.annotation.componentlistpanes.codewords.CodewordButton;
import semgen.annotation.componentlistpanes.submodels.SubmodelButton;
import semgen.annotation.dialog.HumanDefEditor;
import semgen.annotation.dialog.referenceclass.SingularAnnotationEditor;
import semgen.annotation.dialog.selector.SelectorDialogForCodewordsOfSubmodel;
import semgen.annotation.dialog.selector.SelectorDialogForSubmodelsOfSubmodel;
import semgen.annotation.routines.AnnotationCopier;
import semgen.annotation.workbench.AnnotatorWorkbench;
import semgen.annotation.workbench.AnnotatorWorkbench.modeledit;
import semgen.utilities.SemGenFont;
import semgen.utilities.SemGenIcon;
import semgen.utilities.uicomponent.SemGenSeparator;
import semsim.Annotatable;
import semsim.SemSimConstants;
import semsim.annotation.ReferenceOntologyAnnotation;
import semsim.annotation.StructuralRelation;
import semsim.model.Importable;
import semsim.model.SemSimComponent;
import semsim.model.computational.datastructures.DataStructure;
import semsim.model.computational.datastructures.MappableVariable;
import semsim.model.physical.PhysicalEntity;
import semsim.model.physical.PhysicalModelComponent;
import semsim.model.physical.Submodel;
import semsim.model.physical.object.FunctionalSubmodel;
import semsim.owl.SemSimOWLFactory;
import semsim.writing.CaseInsensitiveComparator;

public class AnnotationPanel extends JPanel implements MouseListener, Observer{

	private static final long serialVersionUID = -7946871333815617810L;
	private AnnotatorWorkbench workbench;
	public AnnotatorTab annotator;
	public SemSimComponent smc;
	public AnnotationObjectButton thebutton;
	public CompositeAnnotationPanel compositepanel;
	public SemSimComponentAnnotationPanel singularannpanel;
	private JLabel codewordlabel;
	public AnnotationClickableTextPane subtitlefield;
	public AnnotationClickableTextPane nestedsubmodelpane;
	private JLabel singularannlabel = new JLabel("Singular annotation");
	private AnnotationClickableTextPane humandefpane;
	public JLabel humremovebutton = new JLabel(SemGenIcon.eraseiconsmall);
	private JLabel copyannsbutton = new JLabel(SemGenIcon.copyicon);
	private JLabel loadsourcemodelbutton = new JLabel(SemGenIcon.annotatoricon);
	private Set<DataStructure> cdwdsfromcomps;
	protected SemGenSettings settings;
	GlobalActions globalacts;
	
	public int indent = 15;

	public AnnotationPanel(AnnotatorWorkbench wb, AnnotatorTab ann, SemGenSettings sets, AnnotationObjectButton aob, GlobalActions gacts) throws IOException{
		workbench = wb;
		settings = sets;
		globalacts = gacts;
		annotator = ann;
		thebutton = aob;
		
		workbench.addObserver(this);
		
		String codeword = aob.getName();
		
		if(aob instanceof SubmodelButton)
			smc = ((SubmodelButton)aob).sub;
		else smc = ((CodewordButton)aob).ds;
		
		setBackground(SemGenSettings.lightblue);
		setLayout(new BorderLayout());

		codewordlabel = new JLabel(codeword);
		codewordlabel.setBorder(BorderFactory.createEmptyBorder(5, indent, 5, 10));
		codewordlabel.setFont(SemGenFont.defaultBold(3));
		
		FormatButton(humremovebutton, "Remove this annotation", thebutton.editable);
		
		copyannsbutton.setToolTipText("Copy annotations to all mapped variables");
		copyannsbutton.addMouseListener(this);
		copyannsbutton.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		
		loadsourcemodelbutton.setToolTipText("Annotate source model for this imported sub-model");
		loadsourcemodelbutton.addMouseListener(this);
		loadsourcemodelbutton.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));

		JPanel humandefpanel = new JPanel(new BorderLayout());
		humandefpanel.setBackground(SemGenSettings.lightblue);
		humandefpanel.setBorder(BorderFactory.createEmptyBorder(0, indent, 0, 0));

		JPanel humandefsubpanel = new JPanel();
		humandefsubpanel.setBackground(SemGenSettings.lightblue);
		humandefsubpanel.setLayout(new BoxLayout(humandefsubpanel, BoxLayout.X_AXIS));
		humandefpane = new AnnotationClickableTextPane("[unspecified]",indent, thebutton.editable);
		humandefpane.setAlignmentX(JTextArea.LEFT_ALIGNMENT);
		humandefpane.addMouseListener(this);
		humandefsubpanel.add(humandefpane);
		humandefsubpanel.add(humremovebutton);
		humandefpanel.add(humandefsubpanel, BorderLayout.WEST);
		humandefpanel.add(Box.createGlue(), BorderLayout.EAST);
		
		JPanel subtitlepanel = new JPanel();
		subtitlepanel.setLayout(new BoxLayout(subtitlepanel, BoxLayout.Y_AXIS));
		
		JPanel codewordspanel = new JPanel(new BorderLayout());
		JEditorPane eqpane = null;
		
		// If viewing a submodel
		if(thebutton instanceof SubmodelButton){
			if(thebutton.editable) codewordlabel.addMouseListener(this);
			subtitlefield = new AnnotationClickableTextPane("", 2*indent, (thebutton.editable && !(thebutton.ssc instanceof FunctionalSubmodel)));
			nestedsubmodelpane = new AnnotationClickableTextPane("", 2*indent, (thebutton.editable && !(thebutton.ssc instanceof FunctionalSubmodel)));
			
			codewordspanel.add(subtitlefield, BorderLayout.NORTH);
			codewordspanel.add(Box.createHorizontalGlue(), BorderLayout.EAST);
			codewordspanel.setBackground(SemGenSettings.lightblue);
			subtitlepanel.add(codewordspanel);
			
			JPanel nestedsubmodelpanel = new JPanel(new BorderLayout());
			nestedsubmodelpanel.add(nestedsubmodelpane, BorderLayout.WEST);
			nestedsubmodelpanel.add(Box.createGlue(), BorderLayout.EAST);
			nestedsubmodelpanel.setBackground(SemGenSettings.lightblue);
			subtitlepanel.add(nestedsubmodelpanel);
		}
		// If viewing a codeword, get the equation and units associated with the codeword
		else{
			String code = "";
			if(workbench.getSemSimModel().getDataStructure(codeword).getComputation()!=null){
				code = workbench.getSemSimModel().getDataStructure(codeword).getComputation().getComputationalCode();
			}
			eqpane = new JEditorPane();
			eqpane.setEditable(false);
			eqpane.setText(code);
			eqpane.setFont(SemGenFont.defaultItalic(-1));
			eqpane.setOpaque(false);
			eqpane.setAlignmentX(JEditorPane.LEFT_ALIGNMENT);
			eqpane.setBorder(BorderFactory.createEmptyBorder(2, 2*indent, 2, 2));
			eqpane.setBackground(new Color(0,0,0,0));

			String units = "dimensionless";
			DataStructure ds = (DataStructure)thebutton.ssc;
			if(ds.hasUnits())
				units = ((DataStructure)thebutton.ssc).getUnit().getName();
			codewordlabel.setText(codewordlabel.getText() + " (" + units + ")");
			compositepanel = new CompositeAnnotationPanel(workbench, BoxLayout.Y_AXIS, settings, this, ds);
		}
		subtitlepanel.setBorder(BorderFactory.createEmptyBorder(0, indent, 0, indent));
		subtitlepanel.setBackground(SemGenSettings.lightblue);
		
		singularannlabel.setFont(SemGenFont.defaultBold());
		singularannlabel.setBorder(BorderFactory.createEmptyBorder(10, indent, 5, 0));

		refreshData();

		JPanel mainpanel = new JPanel();
		mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.Y_AXIS));
		
		Box mainheader = Box.createHorizontalBox();
		mainheader.setBackground(SemGenSettings.lightblue);
		mainheader.setAlignmentX(LEFT_ALIGNMENT);
		
		codewordlabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		mainheader.add(codewordlabel);

		if(smc instanceof MappableVariable){
			MappableVariable mvar = (MappableVariable)smc;
			if(!mvar.getMappedTo().isEmpty() || !mvar.getMappedFrom().isEmpty()){
				mainheader.add(copyannsbutton);
				codewordlabel.setBorder(BorderFactory.createEmptyBorder(5, indent, 5, 10));
			}
		}
		// If we're looking at an imported submodel
		else if(smc instanceof Submodel){
			Submodel fsub = (Submodel)smc;
			if(fsub.isImported()){
				mainheader.add(loadsourcemodelbutton);
				codewordlabel.setBorder(BorderFactory.createEmptyBorder(5, indent, 5, 10));
			}
		}
		mainheader.add(Box.createGlue());
		
		mainpanel.add(mainheader);
		mainpanel.add(humandefpanel);
		
		if(thebutton instanceof SubmodelButton){
			subtitlefield.setAlignmentX(JPanel.LEFT_ALIGNMENT);
			codewordspanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
			mainpanel.add(subtitlefield);
			nestedsubmodelpane.setAlignmentX(JPanel.LEFT_ALIGNMENT);
			mainpanel.add(nestedsubmodelpane);
			mainpanel.add(new SemGenSeparator());
		}
		else{
			mainpanel.add(eqpane);
			mainpanel.add(new SemGenSeparator());
			
			JLabel compositelabel = new JLabel("Composite annotation");
			compositelabel.setFont(SemGenFont.defaultBold());
			compositelabel.setBorder(BorderFactory.createEmptyBorder(10, indent, 0, 0));
			
			mainpanel.add(compositelabel);
			
			mainpanel.add(Box.createGlue());
			mainpanel.add(compositepanel);
			mainpanel.add(Box.createGlue());
			mainpanel.add(new SemGenSeparator());
		}
		humandefpanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		singularannpanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		mainpanel.add(singularannlabel);
		mainpanel.add(singularannpanel);
		mainpanel.setBackground(SemGenSettings.lightblue);
		
		add(mainpanel, BorderLayout.NORTH);
		add(Box.createVerticalGlue(), BorderLayout.SOUTH);
		
		setVisible(true);
		ann.annotatorscrollpane.scrollToLeft();
		this.validate();
		this.repaint();
	}

	public void FormatButton(JLabel label, String tooltip, Boolean enabled) {
		label.addMouseListener(this);
		label.setToolTipText(tooltip);
		label.setEnabled(enabled);
		label.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
	}

	public void refreshData() {
		// If a submodel, refresh the associated codewords
		if(thebutton instanceof SubmodelButton)
			refreshSubmodelData();
		// Otherwise we're looking at a codewordbutton - get the composite annotation
		else compositepanel.refreshUI();;
		refreshHumanReadableDefinition();
		refreshSingularAnnotation();
		validate();
		repaint();
	}
	
	public void refreshSubmodelData(){
		
		String subtitletext = "Click to assign codewords to this component";
		String editcomptext = "Click to assign sub-models";
		
		Submodel submod = workbench.getSemSimModel().getSubmodel(thebutton.getName());
		
		if(thebutton.ssc instanceof FunctionalSubmodel){
			subtitletext = "No codewords associated with submodel";
			editcomptext = "No submodels associated with this submodel";
		}
		
		Set<DataStructure> associateddss = new HashSet<DataStructure>(); //submod.getAssociatedDataStructures();
		associateddss.addAll(submod.getAssociatedDataStructures());
		
		Set<Submodel> associatedsubmodels = submod.getSubmodels();
		
		// Include the codewords that are in the subcomponents in the list of associated codewords
		cdwdsfromcomps = SemSimOWLFactory.getCodewordsAssociatedWithNestedSubmodels(submod);
		associateddss.addAll(cdwdsfromcomps);

		if(!associateddss.isEmpty())
			setSubmodelDataOnScreen(submod, subtitlefield, associateddss, "Codewords");
		else{
			subtitlefield.setCustomText(subtitletext);
			subtitlefield.setForeground(Color.gray);
		}
		if(!associatedsubmodels.isEmpty()){
			setSubmodelDataOnScreen(submod, nestedsubmodelpane, associatedsubmodels, "Sub-components");
		}
		else{
			nestedsubmodelpane.setCustomText(editcomptext);
			nestedsubmodelpane.setForeground(Color.gray);
		}
		annotator.annotatorscrollpane.scrollToLeft();
	}
	
	public void refreshHumanReadableDefinition(){
		// Refresh the human readable definition
		String comment = smc.getDescription();

		// Get the human readable definition for the codeword
		if (!comment.equals("")) {
			humandefpane.setCustomText(comment);
			humandefpane.setForeground(Color.blue);
			// Refresh the indicator icons next to the codeword in the bottom left of the Annotator
			thebutton.annotationAdded(thebutton.humdeflabel, false);
			humremovebutton.setEnabled(thebutton.editable);
		} else {
			String msg = "Click to set free-text description";
			if(SemSimComponentIsImported(smc)) msg = "No free-text description specified";
			
			humandefpane.setCustomText(msg);
			humandefpane.setForeground(Color.gray);
			// Refresh the indicator icons next to the codeword in the bottom left of the Annotator
			thebutton.annotationNotAdded(thebutton.humdeflabel);
			humremovebutton.setEnabled(false);
		}
		annotator.updateTreeNode();
	}
	
	public void refreshSingularAnnotation(){
		// Get the singular annotation for the codeword
		singularannpanel = new SemSimComponentAnnotationPanel(workbench, this, settings, (Annotatable)smc);
		singularannpanel.setBorder(BorderFactory.createEmptyBorder(0, indent+5, 0, 0));
		annotator.updateTreeNode();
	}
	
	public void setSubmodelDataOnScreen(Submodel sub, AnnotationClickableTextPane pane, Set<? extends SemSimComponent> associatedsscs, String title){	
		if(thebutton.ssc instanceof FunctionalSubmodel) pane.setForeground(Color.black);
		else pane.setForeground(Color.blue);
		
		String text = "";
		if(!associatedsscs.isEmpty()){
			// Weed out null data structures and sub-models
			ArrayList<SemSimComponent> templist = new ArrayList<SemSimComponent>();
			for(SemSimComponent ssc : associatedsscs){
				if(ssc!=null) templist.add(ssc);
			}
			String[] sarray = new String[templist.size()];
			for(int y=0;y<sarray.length;y++){
				sarray[y] = templist.get(y).getName();
			}
			Arrays.sort(sarray, new CaseInsensitiveComparator());
			for(String s : sarray){
				String name = s;
				if(sub instanceof FunctionalSubmodel){ // Get rid of prepended submodel names if submodel is functional
					name = name.substring(name.lastIndexOf(".")+1);
				}
				if(cdwdsfromcomps.contains(workbench.getSemSimModel().getDataStructure(s)))
					text = text + ", " + "{" + name + "}";
				else
					text = text + ", " + name;
			}
		}
		text = title + ": " + text.substring(2);
		pane.setCustomText(text);
	}

	public Boolean validateNewComponentName(String newname){
		return (!newname.equals("") && !annotator.submodelbuttontable.containsKey(newname) &&
			!annotator.codewordbuttontable.containsKey(newname) && !newname.contains("--"));
	}
	
	public void updateCompositeAnnotationFromUIComponents() throws OWLException{
		annotator.setModelSaved(false);
		DataStructure ds = (DataStructure)smc;

		ArrayList<PhysicalModelComponent> pmclist = new ArrayList<PhysicalModelComponent>();
		ArrayList<StructuralRelation> structuralrellist = new ArrayList<StructuralRelation>();
		compositepanel.validate();
		
		for(int j=0; j<compositepanel.getComponentCount(); j++){
			if(compositepanel.getComponent(j) instanceof SemSimComponentAnnotationPanel){
				Annotatable smc = ((SemSimComponentAnnotationPanel)compositepanel.getComponent(j)).smc;
				if(smc instanceof PhysicalModelComponent) pmclist.add((PhysicalModelComponent)smc);
			}
			if(compositepanel.getComponent(j) instanceof StructuralRelationPanel)
				structuralrellist.add(((StructuralRelationPanel)compositepanel.getComponent(j)).structuralRelation);
		}
		
		// If one property and one property target (works for single entities and processes)
		if(pmclist.size()==2){
			ds.getPhysicalProperty().setPhysicalPropertyOf(pmclist.get(1));
		}
		else if(pmclist.size()>2){
			pmclist.remove(0);
			ArrayList<PhysicalEntity> entlist = new ArrayList<PhysicalEntity>();
			ListIterator<PhysicalModelComponent> iterator = pmclist.listIterator();
			while(iterator.hasNext()) entlist.add((PhysicalEntity)iterator.next());
			ds.getPhysicalProperty().setPhysicalPropertyOf(workbench.getSemSimModel().addCompositePhysicalEntity(entlist, structuralrellist));
		}
		else if(pmclist.size()==1){
			ds.getPhysicalProperty().setPhysicalPropertyOf(null);
			// If there is only a property panel present
		}
		
		if(thebutton.refreshAllCodes()){
			if(settings.organizeByPropertyType() && !settings.useTreeView()) // Not able to sort codewords by marker in tree view yet
				annotator.AlphabetizeAndSetCodewords();
			if(!settings.useTreeView())
				annotator.codewordscrollpane.scrollToComponent(thebutton);
		}
	}
	
	public void showSingularAnnotationEditor(){
		new SingularAnnotationEditor(workbench, this, new Object[]{"Annotate","Close"}) { 
			private static final long serialVersionUID = 1L;

			public void propertyChange(PropertyChangeEvent arg0) {
				String propertyfired = arg0.getPropertyName();
				if (propertyfired.equals("value")) {
					String value = optionPane.getValue().toString();

					//	 If we're using this dialog to apply a non-composite annotation
					if(value == "Annotate" && this.getFocusOwner() != refclasspanel.findbox){
						addClassToOntology();
						workbench.setModelSaved(false);
					}
					dispose();
				}
			}
			
			public void addClassToOntology() {
				if (refclasspanel.getSelection() != null) {
					String selectedname = refclasspanel.getSelection();
					String referenceuri = refclasspanel.getSelectionURI();
					selectedname = selectedname.replaceAll("\"", "");
					ReferenceOntologyAnnotation ann = new ReferenceOntologyAnnotation(SemSimConstants.REFERS_TO_RELATION, URI.create(referenceuri), selectedname);
					singularannpanel.applyReferenceOntologyAnnotation(ann, true);
				}
				singularannpanel.refreshComboBoxItemsAndButtonVisibility();
			}
		};
	}	

	public boolean SemSimComponentIsImported(SemSimComponent comp){
		// If semsim component is imported, change message
		boolean imported = false;
		if(comp instanceof Importable){
			if(((Importable)comp).isImported()) imported = true;;
		}
		else if(comp instanceof DataStructure){
			if(((DataStructure)comp).isImportedViaSubmodel()) imported = true;
		}
		return imported;
	}

	
	public void mouseClicked(MouseEvent arg0) {
		if (arg0.getComponent() == humremovebutton) {
			if(thebutton.editable){
				smc.setDescription("");
				humanDefinitionChanged();
			}
		}
		
		if(arg0.getComponent()==codewordlabel && thebutton instanceof SubmodelButton){
			String newcompname = JOptionPane.showInputDialog(this, "Rename component", annotator.focusbutton.namelabel.getText());
			if(newcompname!=null && !newcompname.equals(codewordlabel.getText())){
				Boolean newnameapproved = validateNewComponentName(newcompname);
				while(!newnameapproved){
					JOptionPane.showMessageDialog(this, "That name is either invalid or already taken");
					newcompname = JOptionPane.showInputDialog(this, "Rename component", newcompname);
					newnameapproved = validateNewComponentName(newcompname);
				}
				thebutton.ssc.setName(newcompname);
				annotator.setModelSaved(false);
				annotator.submodelbuttontable.remove(thebutton.namelabel.getText());
				annotator.submodelbuttontable.put(newcompname, (SubmodelButton)thebutton);
				thebutton.setIdentifyingData(newcompname);
				annotator.changeButtonFocus(thebutton, null);
				annotator.focusbutton = thebutton;
			}
		}
		// Actions for when user clicks on an AnnotationDialogTextArea
		if (arg0.getComponent() == subtitlefield) {
			Submodel sub = ((SubmodelButton)thebutton).sub;
			new SelectorDialogForCodewordsOfSubmodel(
					workbench,
					this,
					workbench.getSemSimModel().getDataStructures(), 
					null,
					sub, 
					sub.getAssociatedDataStructures(),
					SemSimOWLFactory.getCodewordsAssociatedWithNestedSubmodels(sub),
					false,
					"Select codewords");
		}
		
		if( arg0.getComponent() == nestedsubmodelpane){
			Submodel sub = (Submodel)thebutton.ssc;
			new SelectorDialogForSubmodelsOfSubmodel(
					workbench,
					this,
					workbench.getSemSimModel().getSubmodels(),
					sub,
					sub, 
					sub.getSubmodels(),
					null,
					true,
					"Select components");
		}
		
		if (arg0.getComponent() == humandefpane) {
			HumanDefEditor hde = new HumanDefEditor(smc.getName(), smc.getDescription());
			if (!hde.getNewDescription().equals(smc.getDescription())) {
				smc.setDescription(hde.getNewDescription());
				humanDefinitionChanged();
			}
		}
		
		if(arg0.getComponent() == copyannsbutton){
			int x = JOptionPane.showConfirmDialog(this, "Really copy annotations to mapped variables?", "Confirm", JOptionPane.YES_NO_OPTION);
			if(x==JOptionPane.YES_OPTION){
				MappableVariable thevar = (MappableVariable)smc;
				
				for(MappableVariable targetvar : AnnotationCopier.copyAllAnnotationsToMappedVariables(annotator, thevar)){
					annotator.codewordbuttontable.get(targetvar.getName()).refreshAllCodes();
				}
				
				// Update the codeword button markers, re-sort if needed
				if(settings.organizeByPropertyType()){
					annotator.AlphabetizeAndSetCodewords();
					annotator.codewordscrollpane.scrollToComponent(thebutton);
				}
			}
		}
		
		// Activated if user selects the Annotator icon within the AnnotationDialog (used for imported submodels)
		if(arg0.getComponent() == loadsourcemodelbutton){
			File file = new File(annotator.sourcefile.getParent() + "/" + ((Submodel)smc).getHrefValue());

			if(file.exists()){
				globalacts.NewAnnotatorTab(file);
			}
			else{JOptionPane.showMessageDialog(this, "Could not locate source file for this sub-model.", "ERROR", JOptionPane.ERROR_MESSAGE);}
		}
	}

	public void mouseEntered(MouseEvent e) {
		Component component = e.getComponent();
		if(component instanceof JLabel){
			((JLabel)component).setCursor(new Cursor(Cursor.HAND_CURSOR));
		}
		if(component==codewordlabel && thebutton instanceof SubmodelButton){
			codewordlabel.setForeground(Color.blue);
			codewordlabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		}
	}

	public void mouseExited(MouseEvent e) {
		Component component = e.getComponent();
		if(component instanceof JLabel){
			((JLabel)component).setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
		if(component==codewordlabel && thebutton instanceof SubmodelButton){
			codewordlabel.setForeground(Color.black);
			codewordlabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void mousePressed(MouseEvent arg0) {
		Component component = arg0.getComponent();
		if (component instanceof JLabel && component!=codewordlabel) {
			((JLabel)component).setBorder(BorderFactory.createLineBorder(Color.blue,1));
		}
	}
	public void mouseReleased(MouseEvent arg0) {
		Component component = arg0.getComponent();
		if (component instanceof JLabel && component!=codewordlabel) {
			((JLabel)component).setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		}
	}

	public void humanDefinitionChanged() {
		workbench.setModelSaved(false);
		humremovebutton.setEnabled(smc.getDescription()!= "");
		refreshHumanReadableDefinition();
	}
	
	@Override
	public void update(Observable arg0, Object arg1) {
		if (arg1==modeledit.compositechanged) {
			compositepanel.refreshUI();
		}
		
	}
}