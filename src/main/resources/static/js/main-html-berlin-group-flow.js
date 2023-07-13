function getAccountDetailsBG(button) {
    let resultBox = $(button).siblings('.account_detail_bg').empty().append('<h3>Account Detail:</h3>');
    let accountId = $(button).attr('account_id');
    $.getJSON('/account_bg/' + accountId, function (data) {
        let zson = JSON.stringify(data, null, 2);
        let iconId = "result_copy_icon_" + accountId + button.id;
        let resultBoxId = "result_box_" + accountId + button.id;
        resultBox.append(`<div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre>`).append('<br>');
    });
};
function getBalancesBG(button) {
    let resultBox = $(button).siblings('.balances_bg').empty().append('<h3>Balance List:</h3>');
    let accountId = $(button).attr('account_id');
    $.getJSON('/balances_bg/account_id/' + accountId, function (data) {
        let zson = JSON.stringify(data, null, 2);
        let iconId = "result_copy_icon_" + accountId + button.id;
        let resultBoxId = "result_box_" + accountId + button.id;
        resultBox.append(`<div id=${iconId} style="cursor:pointer;" onclick="copyJsonResultToClipboard(this)" class="fa-solid fa-copy"></div><pre><div id=${resultBoxId}>${zson}</div></pre>`).append('<br>');
    });
};
function getTransactionsBG(button) {
    let resultBox = $(button).siblings('.transactions_bg').empty().append('<h3>Transaction List:</h3>');
    let accountId = $(button).attr('account_id');
    $.getJSON('/transactions_bg/account_id/' + accountId, function (data) {
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
    $('#mtls_client_cert_info_bg').click(function () {   
        $.getJSON('/mtls_client_cert_info', function (data) {
            const container = $('#mtls_client_cert_info_bg_div')
            let zson = JSON.stringify(data, null, 2);
            container.empty().append(`<pre>${zson}</pre>`).append('<br>');
        });
    });
    $('#get_accounts_bg').click(function () {
        $.getJSON("/account_bg", function (data) {
            const container = $('#account_list_bg').empty().append('<h1>Account List:</h1>');
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
                    <button onclick="getAccountDetailsBG(this)" id="get_account_detail_bg_${account['resourceId']}" class="btn btn-success" account_id="${account['resourceId']}" >Get Account detail</button>
                    <button onclick="getBalancesBG(this)" id="get_balances_bg_${account['resourceId']}" class="btn btn-warning" account_id="${account['resourceId']}" >Get Balances</button>
                    <button onclick="getTransactionsBG(this)" id="get_transactions_bg_${account['resourceId']}" class="btn btn-info" account_id="${account['resourceId']}" >Get Transactions</button>
                    <div class="account_detail_bg" style="margin-left: 50px;"></div>
                    <div class="balances_bg" style="margin-left: 50px;"></div>
                    <div class="transactions_bg" style="margin-left: 50px;"></div>
                    <hr>
                </div>
            `);
            });
        });
    });
});