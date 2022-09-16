import React from 'react'
import { AuthProvider, AuthConsumer, withAuth } from '../Authentication'
import { BrowserRouter, Route, Link, Switch } from 'react-router-dom'
import { CookiesProvider } from 'react-cookie'
import { RootMenu, MenuItem, MenuLink } from '../components'
import FeatureMainMenuComponent from '../FeatureMainMenu/feature-main-menu.js'
//account
import HalamanDaftarAkunComponent from '../HalamanDaftarAkun/halaman-daftar-akun.js'
//Program
import DaftarProgramComponent from '../DaftarProgram/daftar-program.js'
import HalamanTambahProgramComponent from '../HalamanTambahProgram/halaman-tambah-program.js'
import HalamanDetailProgramComponent from '../HalamanDetailProgram/halaman-detail-program.js'
import HalamanUbahProgramComponent from '../HalamanUbahProgram/halaman-ubah-program.js'

//Income
import CatatanPemasukanComponent from '../CatatanPemasukan/catatan-pemasukan.js'
import HalamanTambahPemasukanComponent from '../HalamanTambahPemasukan/halaman-tambah-pemasukan.js'
import HalamanDetailPemasukanComponent from '../HalamanDetailPemasukan/halaman-detail-pemasukan.js'
import HalamanUbahPemasukanComponent from '../HalamanUbahPemasukan/halaman-ubah-pemasukan.js'
//COA
import LaporanArusKasComponent from '../LaporanArusKas/laporan-arus-kas.js'
//Summary
import CatatanTransaksiComponent from '../CatatanTransaksi/catatan-transaksi.js'
//Expense
import CatatanPengeluaranComponent from '../CatatanPengeluaran/catatan-pengeluaran'
import HalamanTambahPengeluaranComponent from '../HalamanTambahPengeluaran/halaman-tambah-pengeluaran'
import HalamanUbahPengeluaranComponent from '../HalamanUbahPengeluaran/halaman-ubah-pengeluaran'
import HalamanDetailPengeluaranComponent from '../HalamanDetailPengeluaran/halaman-detail-pengeluaran'
//Donation
import HalamanKonfirmasiDonasiOfflineComponent from '../HalamanKonfirmasiDonasiOffline/halaman-konfirmasi-donasi-offline'
import HalamanBerhasilKonfirmasiComponent from '../HalamanBerhasilKonfirmasi/halaman-berhasil-konfirmasi'

import LoginComponent from '../Login/login.js'
import HomepageComponent from '../Homepage/homepage.js'
import LandingPageComponent from '../LandingPage/landing-page.js'

class App extends React.Component {
  state = {}
  onLogoutClicked = e => {
    e.preventDefault()
    this.props.logout()
  }

  currencyFormatDE(num) {
    if (!num) {
      return 0
    }
    return num
  }

  indentBasedOnLevel(content, level) {
    switch (level) {
      case 1:
        return <span>&nbsp;&nbsp;&nbsp;&nbsp;{content}</span>
      case 2:
        return (
          <span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{content}</span>
        )
      case 5:
        return (
          <span>
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            {content}
          </span>
        )
      default:
        return content
    }
  }

  setDistinctRow(content) {
    const DISTINCT_ROW = [
      'Aktivitas Operasional',
      'Aktivitas Investasi',
      'Aktivitas Pendanaan',
      '',
    ]
    if (DISTINCT_ROW.indexOf(content) > -1) {
      return true
    }
    return false
  }

  render() {
    return (
      <CookiesProvider>
        <BrowserRouter>
          <AuthProvider>
            <React.Fragment>
              <AuthConsumer>
                {values => {
                  this.props = values
                  console.log(values)
                  if (values.isAuth) {
                    const variant = {
                      bgColor: 'blue',
                      submenuBgColor: 'lightblue',
                      submenuHoverBgColor: 'lightblue',
                      submenuItemBgColor: 'blue',
                      submenuColor: 'white',
                      uppercase: 'no',
                      itemBgColor: 'darkblue',
                      color: 'white',
                    }
                    return <FeatureMainMenuComponent variant={variant} />
                  } else {
                    return <HomepageComponent />
                  }
                }}
              </AuthConsumer>
              <Switch>
                {/* account */}
                <Route
                  exact
                  path="/halaman-daftar-akun"
                  component={withAuth(HalamanDaftarAkunComponent)}
                />
                {/* Program */}
                <Route
                  exact
                  path="/daftar-program"
                  component={withAuth(DaftarProgramComponent)}
                />
                <Route
                  exact
                  path="/halaman-tambah-program"
                  component={withAuth(HalamanTambahProgramComponent)}
                />
                <Route
                  exact
                  path="/halaman-detail-program"
                  component={withAuth(HalamanDetailProgramComponent)}
                />
                <Route
                  exact
                  path="/halaman-ubah-program"
                  component={withAuth(HalamanUbahProgramComponent)}
                />
                {/* Income */}
                <Route
                  path="/catatan-pemasukan"
                  component={withAuth(CatatanPemasukanComponent)}
                />
                <Route
                  exact
                  path="/halaman-tambah-pemasukan"
                  component={withAuth(HalamanTambahPemasukanComponent)}
                />
                <Route
                  exact
                  path="/halaman-ubah-pemasukan"
                  component={withAuth(HalamanUbahPemasukanComponent)}
                />
                <Route
                  exact
                  path="/halaman-detail-pemasukan"
                  component={withAuth(HalamanDetailPemasukanComponent)}
                />
                {/* COA */}
                <Route
                  exact
                  path="/laporan-arus-kas"
                  component={withAuth(LaporanArusKasComponent)}
                />
                {/* Summary */}
                <Route
                  path="/catatan-transaksi"
                  component={withAuth(CatatanTransaksiComponent)}
                />
                {/* Expense */}
                <Route
                  path="/catatan-pengeluaran"
                  component={withAuth(CatatanPengeluaranComponent)}
                />
                <Route
                  exact
                  path="/halaman-tambah-pengeluaran"
                  component={withAuth(HalamanTambahPengeluaranComponent)}
                />
                <Route
                  exact
                  path="/halaman-ubah-pengeluaran"
                  component={withAuth(HalamanUbahPengeluaranComponent)}
                />
                <Route
                  exact
                  path="/halaman-detail-pengeluaran"
                  component={withAuth(HalamanDetailPengeluaranComponent)}
                />
                {/* Donation */}
                <Route
                  exact
                  path="/halaman-berhasil-Konfirmasi"
                  component={withAuth(HalamanBerhasilKonfirmasiComponent)}
                />
                <Route
                  exact
                  path="/halaman-konfirmasi-donasi-offline"
                  component={withAuth(HalamanKonfirmasiDonasiOfflineComponent)}
                />

                <Route exact path="/login" component={LoginComponent} />
                <Route exact path="/" component={LandingPageComponent} />
              </Switch>
            </React.Fragment>
          </AuthProvider>
        </BrowserRouter>
      </CookiesProvider>
    )
  }
}

export default App
