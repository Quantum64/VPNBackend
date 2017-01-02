package co.q64.vpn.page;

import org.watertemplate.Template;
import org.watertemplate.TemplateMap.SubTemplates;

public class DemoPage extends Template {

	@Override
	protected void addSubTemplates(SubTemplates subTemplates) {
		subTemplates.add("why", new WhySection());
	}

	@Override
	protected String getFilePath() {
		return "demo.water";
	}
}
