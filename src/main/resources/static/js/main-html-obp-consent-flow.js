function createTransactioRequestObp(button) {
    let resultBox = $(button).siblings('.payments_obp').empty().append('<h3>Response:</h3>');
    let bankId = $(button).attr('bank_id');
    let accountId = $(button).attr('account_id');
    const viewHtmlId = "views-" + accountId;
    const selectedViewId = $('#' + viewHtmlId).find(":selected").text();
    let counterpartyId = document.getElementById("creditor_counterparty_obp_" + accountId).value;
    let amount = document.getElementById("obp_payment_amount_of_money_" + accountId).value;
    let currency = document.getElementById("obp_payment_currency_" + accountId).value;
    let description = document.getElementById("obp_payment_description_" + accountId).value;
    
    // The data to be sent to the server
    var data = {
                 to: {
                   counterparty_id: counterpartyId
                 },
                 value: {
                   currency: currency,
                   amount: amount
                 },
                 description: description,
                 charge_policy: "SHARED",
                 future_date: "20200127"
               };
    
    // URL to which the request is sent
    var url = '/payment_obp/' + bankId + '/' + accountId + "/" + selectedViewId + "/" + 
    data.to.counterparty_id + "/" + 
    data.value.currency + "/" + data.value.amount + "/" + 
    data.description + "/" + 
    data.charge_policy + "/" +
    data.future_date;
    
    function setResult(dataToSend) {
        let zson = JSON.stringify(dataToSend, null, 2);
        let iconId = "result_copy_icon_" + accountId + button.id;
        let resultBoxId = "result_box_" + accountId + button.id;
        resultBox.append(`<div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre>`).append('<br>');    
    }
    function setPathOfCall(path) {
        document.getElementById("path-of-endpoint-" + accountId).textContent = path;
    }
    
    // Sending the GET request
    $.ajax({
        url: url,
        method: 'GET',
        dataType: 'json',
        success: function(receivedData, textStatus, jqXHR) {
            // Handle the response data here
            console.log(receivedData);
            setResult(receivedData);
            // Access response headers
            const customHeader = jqXHR.getResponseHeader('Path-Of-Call');
            setPathOfCall(customHeader);
            console.log('Path-Of-Call:', customHeader);
        },
        error: function(jqXHR, textStatus, errorThrown) {
            console.log('Request Failed:', textStatus, errorThrown);
            setResult(jqXHR.responseJSON);
                   
            // Access response headers
            const customHeader = jqXHR.getResponseHeader('Path-Of-Call');
            setPathOfCall(customHeader);
            console.log('Path-Of-Call:', customHeader);
        }
    });

};

function getAccountDetails(button) {
    let resultBox = $(button).siblings('.account_detail_obp').empty().append('<h3>Account Detail:</h3>');
    let accountId = $(button).attr('account_id');
    let bankId = $(button).attr('bank_id');
    const viewHtmlId = "views-" + accountId;
    const selectedViewId = $('#' + viewHtmlId).find(":selected").text();
    $.getJSON('/account_obp/' + bankId + '/' + accountId + '/' + selectedViewId, function (data) {
        let zson = JSON.stringify(data, null, 2);
        let iconId = "result_copy_icon_" + accountId + button.id;
        let resultBoxId = "result_box_" + accountId + button.id;
        resultBox.append(`<div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre>`).append('<br>');
    });
};
function getBalances(button) {
    let resultBox = $(button).siblings('.balances_obp').empty().append('<h3>Balances:</h3>');
    let bankId = $(button).attr('bank_id');
    let accountId = $(button).attr('account_id');
    const viewHtmlId = "views-" + accountId;
    const selectedViewId = $('#' + viewHtmlId).find(":selected").text();
    $.getJSON('/balances_obp/bank_id/' + bankId + '/account_id/' + accountId + "/view_id/" + selectedViewId, function (data) {
        let zson = JSON.stringify(data, null, 2);
        let iconId = "result_copy_icon_" + accountId + button.id;
        let resultBoxId = "result_box_" + accountId + button.id;
        resultBox.append(`<div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre>`).append('<br>');
    });
};
function getTransactions(button) {
    let resultBox = $(button).siblings('.transactions_obp').empty().append('<h3>Transactions:</h3>');
    let bankId = $(button).attr('bank_id');
    let accountId = $(button).attr('account_id');
    const viewHtmlId = "views-" + accountId;
    const selectedViewId = $('#' + viewHtmlId).find(":selected").text();
    $.getJSON('/transactions_obp/bank_id/' + bankId + '/account_id/' + accountId + "/view_id/" + selectedViewId, function (data) {
        let zson = JSON.stringify(data, null, 2);
        let iconId = "result_copy_icon_" + accountId + button.id;
        let resultBoxId = "result_box_"  + accountId + button.id;
        resultBox.append(`<div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre>`).append('<br>');
    });
};
$(function () {
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
    $('#consent_info_obp').click(function () {   
        $.getJSON('/consent_info', function (data) {
            const container = $('#consent_info_obp_div')
            let zson = JSON.stringify(data, null, 2);
            container.empty().append(`<pre>${zson}</pre>`).append('<br>');
        });
    });
    $('#get_accounts_obp').click(function () {
        $.getJSON("/account_obp", function (data) {
            const container = $('#account_list_obp').empty().append('<h1>Accounts:</h1>');
            if (data.code > 399 ) {
              let zson = JSON.stringify(data, null, 2);
              container.append(`<pre>${zson}</pre>`).append('<br>');
            }
            $.each(data.accounts, function (index, account) {
                let zson = JSON.stringify(account, null, 2);
                const accountId = account['id'];
                let iconId = "result_copy_icon_" + accountId;
                let resultBoxId = "result_box_"  + accountId;
                const viewHtmlId = "views-" + accountId;
                
                container.append(`
                    <div><h2>Account ID: ${accountId}</2></div>
                    <div>
                        <div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre><br>
                        <button onclick="getAccountDetails(this)" id="get_account_detail_obp_${account['id']}" class="btn btn-success" account_id="${account['id']}" bank_id="${account['bank_id']}" >Get Account detail</button>
                        <button onclick="getBalances(this)" id="get_balances_obp_${account['id']}" class="btn btn-warning" account_id="${account['id']}" bank_id="${account['bank_id']}" >Get Balances</button>
                        <button onclick="getTransactions(this)" id="get_transactions_obp_${account['id']}" class="btn btn-info" account_id="${account['id']}" bank_id="${account['bank_id']}" >Get Transactions</button>
                        <button onclick="collapsibleElementEventHandler(make_payment_obp_div_${account['id']})" id="prepare_payment_obp_${account['id']}" class="btn btn-info" account_id="${account['id']}" bank_id="${account['bank_id']}" >Prepare / Hide payment</button>
                        <div class="input-group">
                          <label for=${viewHtmlId}>Choose a view:</label>
                          <select class="form-control" id=${viewHtmlId}></select>
                        </div>
                        
                        <div id="make_payment_obp_div_${account['id']}" class="collapse" style="display: none;  margin-left: 50px;">
                            <hr>
                            <div class="form-group">
                                <label for="creditor_counterparty_obp_${account['id']}">To Counterparty ID</label>
                                <input type="text" name="creditor_counterparty_obp_${account['id']}" id="creditor_counterparty_obp_${account['id']}" class="form-control" >
                            </div>
                            <div class="form-group">
                                <label for="obp_payment_description_${account['id']}">Description</label>
                                <input type="text" name="obp_payment_description_${account['id']}" id="obp_payment_description_${account['id']}" class="form-control" >
                            </div>
                            <div class="form-group">
                                <label for="obp_payment_amount_of_money_${account['id']}">Amount of money</label>
                                <input type="number" min="0" value="0" name="obp_payment_amount_of_money_${account['id']}" id="obp_payment_amount_of_money_${account['id']}" class="form-control">
                            </div>
                            <div class="form-group">
                                <label for="obp_payment_currency_${account['id']}">Currency</label>
                                <input type="text" value="EUR" name="obp_payment_currency_${account['id']}" id="obp_payment_currency_${account['id']}" class="form-control" >
                            </div>
                            <button onclick="createTransactioRequestObp(this)" id="make_payment_obp_${account['id']}" class="btn btn-info" account_id="${account['id']}" bank_id="${account['bank_id']}" result_box_id="${account['id']}">Create Transaction Request</button>
                            <h6 id="path-of-endpoint-${account['id']}"></h6>
                            <div class="payments_obp" style="margin-left: 50px;"></div>
                            <hr>                        
                        </div>
                        
                        <div class="account_detail_obp" style="margin-left: 50px;"></div>
                        <div class="balances_obp" style="margin-left: 50px;"></div>
                        <div class="transactions_obp" style="margin-left: 50px;"></div>
                        
                        <hr>
                    </div>
                `);
                const views = $.each(account.views, function (index, view) {
                       $('#' + viewHtmlId).append(`<option value="${view['id']}">${view['id']}</option>`)
                       }
                       );
            });
        });
    });
});

function collapsibleElementEventHandler(elm) {
    var element = document.getElementById(elm.id);
    if (element.style.display == "none"){
      element.style.display = "block";
    } else {
      element.style.display = "none";
    }
}

window.addEventListener("load", (event) => {
  console.log("Page fully loaded");
});