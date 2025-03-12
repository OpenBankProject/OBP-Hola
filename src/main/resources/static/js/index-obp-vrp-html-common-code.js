function setBankSelection() {
    let bank = document.querySelector("#bank");
    localStorage.setItem('last-bank-selection-obp-vrp-flow', bank.value);
}

function restoreLastBankSelection() {
    if(localStorage.getItem('last-bank-selection-obp-vrp-flow')) {
        let bank = document.querySelector("#bank");
        bank.value = localStorage.getItem('last-bank-selection-obp-vrp-flow');
    }
};

function setBankInAccordanceTo(bankId) {
    let bank = document.querySelector("#bank");
    let bankScheme = document.querySelector("#from_bank_routing_scheme");
    if(bankScheme.value == "OBP") {
        bank.value = bankId.value;
    }
};

window.addEventListener("load", (event) => {
  console.log("Page fully loaded");
  // The function to be executed
  restoreLastBankSelection();
});