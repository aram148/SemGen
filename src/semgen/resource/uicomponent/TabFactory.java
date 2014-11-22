package semgen.resource.uicomponent;

import semgen.GlobalActions;
import semgen.SemGenSettings;

import semgen.resource.Workbench;

public abstract class TabFactory<T extends Workbench> {
	protected SemGenSettings settings;
	protected GlobalActions globalactions;
	
	public TabFactory(SemGenSettings sets, GlobalActions actions) {
		settings = sets; globalactions = actions;
	}
	
	public abstract SemGenTab makeTab(T workbench);

}
