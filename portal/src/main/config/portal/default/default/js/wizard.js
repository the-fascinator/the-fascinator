var wizard_def; // used for holding steps of a wizard
var wizardTabIdentifier;

function formSubmit(transitionAction, objectMetaParams,closeURL) {
	if (transitionAction) {
		action = transitionAction;
		if(objectMetaParams) {
			objectMetadataParams = objectMetaParams;
		}
		// go to portal home
		if(closeURL) {
			closeUrl = closeURL;
		}
		var validationPassed = validateTab(); 
		if(validationPassed) {
			jaffa.form.save();
		}
		
		return validationPassed;
	}	
}

function wizard_init(content_selector, tab_heading_selector, json_url, tabIdentifier) {
	jaffa.ui.changeToTabLayout($(content_selector), tab_heading_selector, "h1.heading-selector", tabIdentifier);
	$('[id="'+tabIdentifier+'"]').hide();
	wizardTabIdentifier = tabIdentifier;
	jQuery.getJSON(json_url, function(data) {	wizard_def = data; setFirstWizardStep()});
}

function setFirstWizardStep() {
	var targetState = null;
	for (step in wizard_def["steps"]) {
   	 targetState = step;
   	 break;
	}
		$('a:contains('+targetState+')').click();
}

function transition_click(e)  {
	var validationPassed = formSubmit($(e).attr('form-action'),$(e).attr('object-metadata-params'),$(e).attr('close-transition'));
	if(validationPassed != false) {
	if ($(e).attr('close-transition')) {
		//do nothing here. redirect will occur in save method
	} else {
		var stateName = $('[id="'+wizardTabIdentifier+'"] > li.ui-state-active').text();
		var transitionName = $(e).attr('transition-name');
		var targetState = wizard_def["steps"][stateName][transitionName];
		// Mimic the click on one of ui-tab-nav tab
		$('a:contains('+targetState+')').click();
	}
	}
	return false;
}
