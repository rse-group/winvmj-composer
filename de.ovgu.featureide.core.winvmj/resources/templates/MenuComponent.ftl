import React from 'react';
import AuthConsumer from '../Authentication';
import { withRouter } from 'react-router-dom';
import RootMenu from '../components/RootMenu/RootMenu';
import MenuItem from '../components/MenuItem/MenuItem';
import MenuLink from '../components/MenuLink/MenuLink';
import MenuChildren from '../components/MenuChildren/MenuChildren';
import FeatureArrow from '../components/FeatureArrow/FeatureArrow';

<#if financialreport??>
import FinancialReportMainMenuComponent from '../FinancialReportMainMenu/financial-report-main-menu.js';
</#if>
<#if income??>
import IncomeMainMenuComponent from '../IncomeMainMenu/income-main-menu.js';
</#if>
<#if expense??>
import ExpenseMainMenuComponent from '../ExpenseMainMenu/expense-main-menu.js';
</#if>
<#if program??>
import ProgramMainMenuComponent from '../ProgramMainMenu/program-main-menu.js';
</#if>
import ReportMainMenuComponent from '../ReportMainMenu/report-main-menu.js';
<#if summary??>
import SummaryMainMenuComponent from '../SummaryMainMenu/summary-main-menu.js';
</#if>
import AutomaticMainMenuComponent from '../AutomaticMainMenu/automatic-main-menu.js';
<#if aruskasreport??>
import CoaMainMenuComponent from '../CoaMainMenu/coa-main-menu.js';
</#if>
import ListDeskripsiCoaMainMenuComponent from '../ListDeskripsiCoaMainMenu/list-deskripsi-coa-main-menu.js';
<#if donation??>
import DonationMainMenuComponent from '../DonationMainMenu/donation-main-menu.js';
import DonasiMainMenuComponent from '../DonasiMainMenu/donasi-main-menu.js';
</#if>
import OfflineMainMenuComponent from '../OfflineMainMenu/offline-main-menu.js';

class FeatureMainMenu extends React.Component {
    state = {};
    
    onLogoutClicked = e => {
            e.preventDefault();
            this.props.logout();
        };
    
    currencyFormatDE(num) {
        if (!num) {
            return "0,00"
        }
        return (
            num.toFixed(2).replace('.', ',').replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1.')
        )
    }

    render() {
		const appVariant = this.props.variant;
        return (
	        <AuthConsumer>{ (values) => 
	        	{ return (
				<RootMenu isAuth={values.isAuth} variant={appVariant}>
					<MenuItem variant={appVariant}>
						<MenuLink variant={appVariant} href="#" onClick={this.onLogoutClicked}>
							Logout
						</MenuLink>
					</MenuItem>
					<ProgramMainMenuComponent {...values} variant={appVariant}/>
					<FinancialReportMainMenuComponent variant={appVariant}>
						<IncomeMainMenuComponent {...values} variant={appVariant}/>
						<ExpenseMainMenuComponent {...values} variant={appVariant}/>
						<ReportMainMenuComponent variant={appVariant}>
							<SummaryMainMenuComponent {...values} variant={appVariant}/>
							<AutomaticMainMenuComponent variant={appVariant}>
								<CoaMainMenuComponent {...values} variant={appVariant}/>
							</AutomaticMainMenuComponent>
							<ListDeskripsiCoaMainMenuComponent {...values} variant={appVariant}/>
						</ReportMainMenuComponent>
					</FinancialReportMainMenuComponent>
					<DonationMainMenuComponent variant={appVariant}>
						<OfflineMainMenuComponent variant={appVariant}>
							<DonasiMainMenuComponent {...values} variant={appVariant}/>
						</OfflineMainMenuComponent>
					</DonationMainMenuComponent>
				</RootMenu>)}
			}</AuthConsumer>
        );
    }
}

export default FeatureMainMenu;