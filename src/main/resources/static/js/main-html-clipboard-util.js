// This function copies the JSON result when we press a copy icon in top left corner.
// In case that action is successful the icon is changed for a 2 seconds in order to notify a user about it.
function copyJsonResultToClipboard(element) {
  var id = String(element.id).replace('result_copy_icon_','result_box_');
  var r = document.createRange();
  r.selectNode(document.getElementById(id));
  window.getSelection().removeAllRanges();
  window.getSelection().addRange(r);
  document.execCommand('copy');
  window.getSelection().removeAllRanges();
  // Store original values
  var titleText = document.getElementById(element.id).title;
  var iconClass = document.getElementById(element.id).className;
  // and then change hey
  document.getElementById(element.id).title = "";
  document.getElementById(element.id).className = "fa-regular fa-copy";
  
  // Below code is GUI related i.e. to notify a user that text is copied to clipboard
  // --------------------------------------------------------------------------------
  
  // It delays the call by ms milliseconds
  function defer(f, ms) {
    return function() {
      setTimeout(() => f.apply(this, arguments), ms);
    };
  }
  
  // Function which revert icon and text to the initial state.
  function revertTextAndClass(titleText, iconClass) {
    document.getElementById(element.id).title = titleText;
    document.getElementById(element.id).className = iconClass
  }
  
  var revertTextAndClassDeferred = defer(revertTextAndClass, 2000);
  // Revert the original values of text and icon after 2 seconds
  revertTextAndClassDeferred(titleText, iconClass); 

}