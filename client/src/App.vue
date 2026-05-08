<script setup>
import LoginDialog from '@/components/LoginDialog.vue'
import { useUserStore } from '@/stores/user' 
const userStore = useUserStore() 
</script>

<template>
  <router-view :key="userStore.token" />
    <LoginDialog /> 
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: Arial, Helvetica, sans-serif;
  background: #f5f5f5;
}

a {
  text-decoration: none;
  color: inherit;
}

ul {
  list-style: none;
}
</style>
