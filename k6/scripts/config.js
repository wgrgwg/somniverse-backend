let fileCfg = {};
try {
  const raw = open('../env/local.json');
  fileCfg = JSON.parse(raw);
} catch (e) {
  fileCfg = {};
}

export const CFG = {
  BASE: fileCfg.BASE,
  EMAIL: fileCfg.EMAIL,
  PASSWORD: fileCfg.PASSWORD
};