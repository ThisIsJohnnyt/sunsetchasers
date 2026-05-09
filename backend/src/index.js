require('dotenv').config();
const express = require('express');
const forecastRouter = require('./routes/forecast');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.use('/api', forecastRouter);

app.listen(PORT, () => {
  console.log(`Sunset Chasers API listening on port ${PORT}`);
});

module.exports = app;
