$(document).ready(function(){if(!("placeholder" in document.createElement("input"))){$("input[placeholder], textarea[placeholder]").each(function(){var b=$(this).attr("placeholder");if(this.value==""){this.value=b}$(this).focus(function(){if(this.value==b){this.value=""}}).blur(function(){if($.trim(this.value)==""){this.value=b}})});$("form").submit(function(){$(this).find("input[placeholder], textarea[placeholder]").each(function(){if(this.value==$(this).attr("placeholder")){this.value=""}})})}$("#signup form").submit(function(){var b=$("#signup #signup-agree-terms input");if(b.length>0){if(!b.prop("checked")){var c="Please agree to the Terms & Conditions";$("#signup #signup-error #error").html(c);$("#signup #signup-error").removeClass("hide");return false}}return true});$("#signup form").submit(function(){var c=$("#signup #signup-agree-privacy-policy input");if(c.length>0){if(!c.prop("checked")){var b="Please agree to the Privacy Policy";$("#signup #signup-error #error").html(b);$("#signup #signup-error").removeClass("hide");return false}}return true});var a=$("#signup #signup-error #error");if(a.length>0&&a.html().length>0){a.parent().removeClass("hide")}});