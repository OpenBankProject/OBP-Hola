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
    let resultBox = $(button).siblings('.balances_obp').empty().append('<h3>Balance List:</h3>');
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
    let resultBox = $(button).siblings('.transactions_obp').empty().append('<h3>Transaction List:</h3>');
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
            const container = $('#account_list_obp').empty().append('<h1>Account List:</h1>');
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
                    <div>
                        <div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre><br>
                        <button onclick="getAccountDetails(this)" id="get_account_detail_obp_${account['id']}" class="btn btn-success" account_id="${account['id']}" bank_id="${account['bank_id']}" >Get Account detail</button>
                        <button onclick="getBalances(this)" id="get_balances_obp_${account['id']}" class="btn btn-warning" account_id="${account['id']}" bank_id="${account['bank_id']}" >Get Balances</button>
                        <button onclick="getTransactions(this)" id="get_transactions_obp_${account['id']}" class="btn btn-info" account_id="${account['id']}" bank_id="${account['bank_id']}" >Get Transactions</button>
                        <div class="input-group">
                          <label for=${viewHtmlId}>Choose a view:</label>
                          <select class="form-control" id=${viewHtmlId}></select>
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