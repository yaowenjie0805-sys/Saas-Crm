import LoginView from '../LoginView'
import InvitationAcceptView from '../InvitationAcceptView'

export default function AuthShell({
  auth,
  locationPathname,
  apiContext,
  lang,
  setLang,
  t,
  navigate,
  submitLogin,
  submitRegister,
  loginForm,
  setLoginForm,
  registerForm,
  setRegisterForm,
  formErrors,
  setFormErrors,
  submitSsoLogin,
  ssoConfig,
  ssoForm,
  setSsoForm,
  oidcAuthorizing,
  startOidcLogin,
  loginError,
}) {
  if (!auth && locationPathname === '/activate') {
    return (
      <InvitationAcceptView
        lang={lang}
        setLang={setLang}
        t={t}
        onBackToLogin={() => navigate('/login')}
        apiContext={apiContext}
      />
    )
  }

  if (!auth) {
    return (
      <LoginView
        lang={lang}
        setLang={setLang}
        t={t}
        submitLogin={submitLogin}
        submitRegister={submitRegister}
        loginForm={loginForm}
        setLoginForm={setLoginForm}
        registerForm={registerForm}
        setRegisterForm={setRegisterForm}
        formErrors={formErrors}
        setFormErrors={setFormErrors}
        submitSsoLogin={submitSsoLogin}
        ssoConfig={ssoConfig}
        ssoForm={ssoForm}
        setSsoForm={setSsoForm}
        oidcAuthorizing={oidcAuthorizing}
        startOidcLogin={startOidcLogin}
        openActivate={() => navigate('/activate')}
        error={loginError}
      />
    )
  }

  return null
}
