/*
 * <<
 * Davinci
 * ==
 * Copyright (C) 2016 - 2017 EDP
 * ==
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * >>
 */

import React, { PureComponent } from 'react'
import PropTypes from 'prop-types'
import Helmet from 'react-helmet'
import { connect } from 'react-redux'
import { createStructuredSelector } from 'reselect'

import Banner from './Banner3'
import LoginForm from './LoginForm'
import Row from 'antd/lib/row'
import Col from 'antd/lib/col'
import Button from 'antd/lib/button'

import { login, logged, setLoginUser } from '../App/actions'
import { makeSelectLoginLoading } from '../App/selectors'
import { promiseDispatcher } from '../../utils/reduxPromisation'
import checkLogin from '../../utils/checkLogin'
import { setToken } from '../../utils/request'

import styles from './Login3.less'

export class Login extends PureComponent {
  componentWillMount () {
    this.checkNormalLogin()
  }

  checkNormalLogin = () => {
    if (checkLogin()) {
      const token = localStorage.getItem('TOKEN')
      const loginUser = localStorage.getItem('loginUser')

      setToken(token)
      this.props.onLogged()
      this.props.onSetLoginUser(JSON.parse(loginUser))
      this.props.router.replace('/')
    }
  }

  doLogin = () => {
    const {
      onLogin,
      router
    } = this.props
    this.loginForm.validateFieldsAndScroll((err, { username, password }) => {
      if (!err) {
        onLogin(username, password, () => { router.replace('/') })
      }
    })
  }

  render () {
    const { loginLoading } = this.props
    return (
      <div className={styles.login}>
        <Helmet title="Login" />
        <div className={styles.logo}>
          <span>D</span>
          <span>a</span>
          <span>v</span>
          <span>i</span>
          <span>n</span>
          <span>c</span>
          <span>i</span>
        </div>
        <Banner />
        <div className={styles.window}>
          <Row gutter={8}>
            <Col sm={21}>
              <LoginForm
                onLogin={this.doLogin}
                ref={f => { this.loginForm = f }}
              />
            </Col>
            <Col sm={3}>
              <Button
                size="large"
                disabled={loginLoading}
                loading={loginLoading}
                onClick={this.doLogin}
              >
                登 录
              </Button>
            </Col>
          </Row>
        </div>
      </div>
    )
  }
}

Login.propTypes = {
  router: PropTypes.any,
  loginLoading: PropTypes.bool,
  onLogin: PropTypes.func,
  onLogged: PropTypes.func,
  onSetLoginUser: PropTypes.func
}

const mapStateToProps = createStructuredSelector({
  loginLoading: makeSelectLoginLoading()
})

export function mapDispatchToProps (dispatch) {
  return {
    onLogin: (username, password, resolve) => dispatch(login(username, password, resolve)),
    onLogged: () => promiseDispatcher(dispatch, logged),
    onSetLoginUser: (user) => promiseDispatcher(dispatch, setLoginUser, user)
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(Login)

