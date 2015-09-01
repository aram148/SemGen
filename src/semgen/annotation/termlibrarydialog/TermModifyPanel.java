package semgen.annotation.termlibrarydialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import semgen.SemGenSettings;
import semgen.annotation.common.CustomTermOptionPane;
import semgen.annotation.common.EntitySelectorGroup;
import semgen.annotation.dialog.termlibrary.CustomPhysicalProcessPanel;
import semgen.annotation.workbench.AnnotatorWorkbench;
import semgen.annotation.workbench.SemSimTermLibrary;

public class TermModifyPanel extends JPanel implements ActionListener, Observer {
	private static final long serialVersionUID = 1L;
	private SemSimTermLibrary library;
	private JPanel modpane;
	private Integer compindex;
	private JPanel modcntrls = new JPanel();
	private JLabel msglbl = new JLabel();
	private JButton confirmbtn = new JButton("Modify");
	
	public TermModifyPanel(AnnotatorWorkbench wb) {
		library = wb.openTermLibrary();
		wb.addObserver(this);
		createGUI();
		setBorder(BorderFactory.createEtchedBorder());
	}
	
	private void createGUI() {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS)); 
		setBackground(SemGenSettings.lightblue);
		modcntrls.setLayout(new BoxLayout(modcntrls, BoxLayout.LINE_AXIS));
		modcntrls.setBackground(SemGenSettings.lightblue);
		modcntrls.setAlignmentX(LEFT_ALIGNMENT);
		modcntrls.add(confirmbtn);
		confirmbtn.addActionListener(this);
		confirmbtn.setEnabled(false);
		modcntrls.add(msglbl);
	}
	
	public void showModifier(int index) {
		compindex = index;
		if (modpane!=null) {
			remove(modpane);
			remove(modcntrls);
		}
		
		switch (library.getSemSimType(compindex)) {
		case CUSTOM_PHYSICAL_ENTITY:
			modpane = new CustomEntityPane(library);
			break;
		case COMPOSITE_PHYSICAL_ENTITY:
			modpane = new CPEPanel();
			break;
		case CUSTOM_PHYSICAL_PROCESS:
			modpane = new CustomProcessPane(library);
			break;
		default:
			break;
		}
		
		add(modpane);
		add(modcntrls);
		validate();
	}
	
	@Override
	public void update(Observable arg0, Object arg1) {
		
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		((TermEditor)modpane).editTerm();
	}
	
	private interface TermEditor {
		public void editTerm();
	}
	
	private class CustomProcessPane extends CustomPhysicalProcessPanel implements TermEditor {
		private static final long serialVersionUID = 1L;

		public CustomProcessPane(SemSimTermLibrary lib) {
			super(lib, compindex);
			remove(confirmpan);
			createbtn = confirmbtn;
			msgbox = msglbl;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {}

		@Override
		public void editTerm() {
			modifyTerm();
			confirmbtn.setEnabled(false);
			msglbl.setText("Custom process modified.");
		}
	}
	
	private class CustomEntityPane extends CustomTermOptionPane implements TermEditor {
		private static final long serialVersionUID = 1L;

		public CustomEntityPane(SemSimTermLibrary lib) {
			super(lib, compindex);
			remove(confirmpan);
			createbtn = confirmbtn;
			msgbox = msglbl;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {}

		@Override
		public void editTerm() {
			modifyTerm();
			confirmbtn.setEnabled(false);
			msglbl.setText("Custom entity modified.");
		}
		
	}
	
	private class CPEPanel extends JPanel implements TermEditor{
		private static final long serialVersionUID = 1L;
		private CompositeCreator cpec;
		
		
		public CPEPanel() {		
			cpec = new CompositeCreator(library);
			setBackground(SemGenSettings.lightblue);
			add(cpec);
		}
		
		@Override
		public void editTerm() {
			cpec.editTerm();
			confirmbtn.setEnabled(false);
			msglbl.setText("Composite entity modified.");
		}
	}
	
	private class CompositeCreator extends EntitySelectorGroup  {
		private static final long serialVersionUID = 1L;
				
		public CompositeCreator(SemSimTermLibrary lib) {
			super(lib,library.getCompositeEntityIndicies(compindex), true);
			
		}

		@Override
		public void onChange() {
			pollSelectors();
			if (selections.contains(-1)) {
				confirmbtn.setEnabled(false);
				msglbl.setText("Composite entities cannot contain unspecified terms.");
			}
			else if (library.containsCompositeEntitywithTerms(selections)) {
				msglbl.setText("Composite physical entity with those terms already exists.");
				confirmbtn.setEnabled(false);
			}
			else {
				confirmbtn.setEnabled(true);
				msglbl.setText("Valid composite physical entity");
			}
		}
		
		public void editTerm() {
			library.setCompositeEntityComponents(compindex, selections);
		}
		
	
	}


}