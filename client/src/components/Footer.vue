<template>
    <footer v-if="homeStore.activeTab === 'deals'" class="site-footer">
      <div class="footer-inner">
        <div class="footer-links">
          <div class="link-group">
            <h4>购物指南</h4>
            <a href="#">购物流程</a>
            <a href="#">会员权益</a>
            <a href="#">积分规则</a>
          </div>
          <div class="link-group">
            <h4>支付与配送</h4>
            <a href="#">支付方式</a>
            <a href="#">配送说明</a>
            <a href="#">运费政策</a>
          </div>
          <div class="link-group">
            <h4>售后服务</h4>
            <a href="#">退换货政策</a>
            <a href="#">保修服务</a>
            <a href="#">联系客服</a>
            <a class="footer-hotline" href="tel:17200697759">售后热线：17200697759</a>
          </div>
          <div class="link-group">
            <h4>关于我们</h4>
            <a href="#">品牌故事</a>
            <a href="#">门店查询</a>
            <a href="#">加入我们</a>
          </div>
          <div class="link-group">
            <h4>隐私与法律</h4>
            <a href="#">隐私政策</a>
            <a href="#">服务条款</a>
            <a href="#">Cookie声明</a>
          </div>
        </div>
        <div class="footer-copyright">
          <p>© 2025 型动派 | 保留所有权利 </p>
          <div class="payment-icons">
            <span>微信支付</span>
            <span>支付宝</span>
            <span>银联</span>
            <span>Visa</span>
          </div>
        </div>
      </div>
    </footer>
</template>

<script setup>
import { useHomeStore } from '@/stores/home'
const homeStore = useHomeStore()
</script>

<style scoped>
.site-footer {
  background: #2c3e50;
  color: #ecf0f1;
  padding: 40px 0 20px;
  margin-top: 40px;
  width: 100%;

  .footer-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .footer-links {
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 30px;
    margin-bottom: 30px;

    .link-group {
      display: flex;
      flex-direction: column;
      gap: 12px;

      h4 {
        font-size: 16px;
        font-weight: bold;
        color: #fff;
      }

      a {
        color: #bdc3c7;
        text-decoration: none;
        font-size: 14px;
        transition: color 0.2s;

        &:hover {
          color: #3498db;
        }
      }
    }
  }

  .footer-copyright {
    border-top: 1px solid #34495e;
    padding-top: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;

    p {
      margin: 0;
      font-size: 13px;
      color: #95a5a6;
    }
  }

  .payment-icons {
    display: flex;
    gap: 20px;

    span {
      font-size: 14px;
      color: #bdc3c7;
      background: #34495e;
      padding: 6px 12px;
      border-radius: 4px;
    }
  }
}

</style>