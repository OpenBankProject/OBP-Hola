function getAccountDetails(button) {
    let accountDetailEle = $(button).siblings('.account_detail_obp').empty().append('<h3>Account Detail:</h3>');
    let accountId = $(button).attr('account_id');
    let bankId = $(button).attr('bank_id');
    $.getJSON('/account_obp/' + bankId + '/' + accountId, function (data) {
        let zson = JSON.stringify(data, null, 2);
        accountDetailEle.append(`<pre>${zson}</pre>`).append('<br>');
    });
};
function getBalances(button) {
    let balancesEle = $(button).siblings('.balances_obp').empty().append('<h3>Balance List:</h3>');
    let bankId = $(button).attr('bank_id');
    let accountId = $(button).attr('account_id');
    $.getJSON('/balances_obp/bank_id/' + bankId + '/account_id/' + accountId, function (data) {
        let zson = JSON.stringify(data, null, 2);
        balancesEle.append(`<pre>${zson}</pre>`).append('<br>');
    });
};
function getTransactions(button) {
    let accountsEle = $(button).siblings('.transactions_obp').empty().append('<h3>Transaction List:</h3>');
    let bankId = $(button).attr('bank_id');
    let accountId = $(button).attr('account_id');
    $.getJSON('/transactions_obp/bank_id/' + bankId + '/account_id/' + accountId, function (data) {
        let zson = JSON.stringify(data, null, 2);
        accountsEle.append(`<pre>${zson}</pre>`).append('<br>');
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
                container.append(`
                <div>
                    <pre>${zson}</pre><br>
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
