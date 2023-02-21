function getAccountDetails(button) {
    let resultBox = $(button).siblings('.account_detail_obp').empty().append('<h3>Account Detail:</h3>');
    let accountId = $(button).attr('account_id');
    let bankId = $(button).attr('bank_id');
    $.getJSON('/account_obp/' + bankId + '/' + accountId, function (data) {
        let zson = JSON.stringify(data, null, 2);
        let iconId = "result_copy_icon_" + accountId + button.id;
        let resultBoxId = "result_box_" + accountId + button.id;
        resultBox.append(`<div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre>`).append('<br>');
    });
};
function getBalances(button) {
    let resultBox = $(button).siblings('.balances_obp').empty().append('<h3>Balance List:</h3>');
    let bankId = $(button).attr('bank_id');
    let accountId = $(button).attr('account_id');
    $.getJSON('/balances_obp/bank_id/' + bankId + '/account_id/' + accountId, function (data) {
        let zson = JSON.stringify(data, null, 2);
        let iconId = "result_copy_icon_" + accountId + button.id;
        let resultBoxId = "result_box_" + accountId + button.id;
        resultBox.append(`<div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre>`).append('<br>');
    });
};
function getTransactions(button) {
    let resultBox = $(button).siblings('.transactions_obp').empty().append('<h3>Transaction List:</h3>');
    let bankId = $(button).attr('bank_id');
    let accountId = $(button).attr('account_id');
    $.getJSON('/transactions_obp/bank_id/' + bankId + '/account_id/' + accountId, function (data) {
        let zson = JSON.stringify(data, null, 2);
        let iconId = "result_copy_icon_" + accountId + button.id;
        let resultBoxId = "result_box_"  + accountId + button.id;
        resultBox.append(`<div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre>`).append('<br>');
    });
};
$(function () {
    $.ajaxSetup({
        "error": function (e) {
            alert(`error: code=${e.status}, fail msg: ${e.responseText}`);
            console.log(e);
        }
    });
    $('#revoke_consent_obp').click(function () {   
        $.getJSON('/revoke_consent_obp/', function (data) {
            const container = $('#revoke_consent_obp_div')
            let zson = JSON.stringify(data, null, 2);
            container.append(`<pre>${zson}</pre>`).append('<br>');
        });
    });
    $('#mtls_client_cert_info_obp').click(function () {   
        $.getJSON('/mtls_client_cert_info', function (data) {
            const container = $('#mtls_client_cert_info_obp_div')
            let zson = JSON.stringify(data, null, 2);
            container.empty().append(`<pre>${zson}</pre>`).append('<br>');
        });
    });
    $('#get_accounts_obp').click(function () {
        $.getJSON("/account_obp", function (data) {
            const container = $('#account_list_obp').empty().append('<h1>Account List:</h1>');
            if (data.code > 399 ) {
              let zson = JSON.stringify(data, null, 2);
              container.append(`<pre>${zson}</pre>`).append('<br>');
            }
            $.each(data.accounts, function (index, account) {
                let zson = JSON.stringify(account, null, 2);
                let iconId = "result_copy_icon_" + account['id'];
                let resultBoxId = "result_box_"  + account['id'];
                container.append(`
                <div>
                    <div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre><br>
                    <button onclick="getAccountDetails(this)" id="get_account_detail_obp_${account['id']}" class="btn btn-success" account_id="${account['id']}" bank_id="${account['bank_id']}" >Get Account detail</button>
                    <button onclick="getBalances(this)" id="get_balances_obp_${account['id']}" class="btn btn-warning" account_id="${account['id']}" bank_id="${account['bank_id']}" >Get Balances</button>
                    <button onclick="getTransactions(this)" id="get_transactions_obp_${account['id']}" class="btn btn-info" account_id="${account['id']}" bank_id="${account['bank_id']}" >Get Transactions</button>
                    <div class="account_detail_obp" style="margin-left: 50px;"></div>
                    <div class="balances_obp" style="margin-left: 50px;"></div>
                    <div class="transactions_obp" style="margin-left: 50px;"></div>
                    <hr>
                </div>
            `);
            });
        });
    });
});

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