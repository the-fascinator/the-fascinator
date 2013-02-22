/*
 * This is modified based on widget2.js come with jaffa library
 * It is a trimmed down version of original file to support datepicker, no others.
 */
var widgets = {};

function prepareHelps() {
	var help_contents = $('.pre-help-content');
	for (var i = 0; i < help_contents.length; i++ ) {
		$(help_contents[i]).hide().attr("class","help-content");
		var helpToggleHtml = "<button class=\"jaffaHelpToggle\"></button>";
	    helpToggle = $(helpToggleHtml);
	    helpToggle.button({icons: {primary:'ui-icon-help'}});

	    // Setup click logic
	    helpToggle.click(function(){$(this).next().toggle('fast');});

	    // And attach to our container
	    helpToggle.insertBefore(help_contents[i]);	
	}
}

(function($){

function datepickerOnClose(dateText, inst) {
    var month = $("#ui-datepicker-div .ui-datepicker-month :selected").val();
    var year = $("#ui-datepicker-div .ui-datepicker-year :selected").val();
    if (!month) {
        month=0;
    }
    $(this).datepicker('setDate', new Date(year, month, 1));
    $(this).blur();
}

function datepickerBeforeShow(input, inst) {
    inst = $(inst.input);
    if (inst.hasClass("dateMY") || inst.hasClass("dateYM") || inst.hasClass("dateY")) {
        setTimeout(function() {
            $(".ui-datepicker-calendar").remove();
            $(".ui-datepicker-current").remove();
            $(".ui-datepicker-close").text("OK");
            if (inst.hasClass("dateY")) {
                $(".ui-datepicker-month").remove();
            }
        }, 10);
    }
}

function contentSetup(ctx) {
    //
    try{
        // ==============
        // Date inputs
        // ==============
        ctx.find("input.dateYMD, input.date").datepicker({
            dateFormat: "yy-mm-dd", 
            changeMonth: true, 
            changeYear: true, 
            showButtonPanel: false
        });
        ctx.find('input.dateYM').datepicker({
            changeMonth: true, 
            changeYear: true, 
            showButtonPanel: true, 
            dateFormat: 'yy-mm',
            onClose: datepickerOnClose,
            beforeShow: datepickerBeforeShow,
            onChangeMonthYear: function(year, month, inst) {
                datepickerBeforeShow(null, inst);
            },
            onSelect: function(dateText, inst) {}
        });
        ctx.find('input.dateMMY').datepicker({
            changeMonth: true, 
            changeYear: true, 
            showButtonPanel: true, 
            dateFormat: 'MM yy',
            onClose: datepickerOnClose,
            beforeShow: datepickerBeforeShow,
            onChangeMonthYear: function(year, month, inst) {
                datepickerBeforeShow(null, inst);
            },
            onSelect: function(dateText, inst) {}
        });
        ctx.find('input.dateY').datepicker({
            changeMonth: false, 
            changeYear: true, 
            showButtonPanel: true, 
            dateFormat: 'yy',
            //onClose: datepickerOnClose,
            onClose: function(dateText) { // for IE7
                var year = $("#ui-datepicker-div .ui-datepicker-year :selected").val();
                $(this).val(year);
            },
            beforeShow: datepickerBeforeShow,
            onChangeMonthYear: function(year, month, inst) {
                datepickerBeforeShow(null, inst);
            },
            onSelect: function(dateText, inst) {}
        });
    } catch(e) {
        alert("Error in contentSetup() - " + e.message);
    }
}

function contentDisable(ctx) {
    ctx.find("input").filter(".dateYMD, .date, .dateYM, .dateMMY, .dateY").datepicker("destroy");
}

function contentLoaded() {
    //alert("contentLoaded");
    contentSetup($("body"));
}

widgets.contentLoaded = contentLoaded;
})(jQuery);
