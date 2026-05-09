const express = require('express');
const router = express.Router();

router.post('/forecast', (req, res) => {
  res.json({ status: 'coming soon', message: 'Forecast endpoint will be implemented in Sprint 1.3' });
});

module.exports = router;
