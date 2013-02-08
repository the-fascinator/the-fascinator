var wizard_def; // used for holding steps of a wizard

function wizard_init(content_selector, tab_heading_selector, json_url) {
	jaffa.ui.changeToTabLayout($(content_selector), tab_heading_selector);
	$('.ui-tabs-nav').hide();
	jQuery.getJSON(json_url, function(data) {	wizard_def = data; });
}

function transition_click(e)  {
	var stateName = $('.ui-tabs-nav > li.ui-state-active').text();
	var transitionName = $(e).attr('transition-name');
	var targetState = wizard_def["steps"][stateName][transitionName];
	$('a:contains('+targetState+')').click();
	return false;
}
